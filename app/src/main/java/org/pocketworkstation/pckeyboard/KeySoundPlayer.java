package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

/**
 * Plays recorded mechanical-switch key sounds.
 *
 * <p>The samples come from the Mechvibes sound packs (MIT licensed, see
 * assets/keysounds/NOTICE.txt). Each pack is stored as seven small WAVs: four
 * letter-key variations plus space, enter and backspace. Assets are copied to the
 * cache directory on first use because SoundPool loads from files only, then kept
 * in a SoundPool for low-latency playback.
 */
public class KeySoundPlayer {
    private static final String TAG = "HK/KeySound";

    /** Android's own keypress tone; no samples involved. */
    public static final int PROFILE_SYSTEM = 0;

    public static final int KIND_KEY = 0;
    public static final int KIND_SPACE = 1;
    public static final int KIND_ENTER = 2;
    public static final int KIND_DELETE = 3;

    /** Asset directory per profile; index 0 is the system tone. */
    private static final String[] PACKS = {
            null,
            "cherrymx-blue-abs",
            "cherrymx-blue-pbt",
            "cherrymx-brown-abs",
            "cherrymx-brown-pbt",
            "cherrymx-red-abs",
            "cherrymx-black-abs",
            "cherrymx-black-pbt",
            "eg-crystal-purple",
            "eg-oreo",
            "topre-purple-hybrid-pbt",
    };

    public static final int PROFILE_COUNT = PACKS.length;

    private static final String[] KEY_FILES = {"key1.wav", "key2.wav", "key3.wav", "key4.wav"};
    private static final int SAMPLE_COUNT = KEY_FILES.length + 3;

    private final Context mContext;
    private final Random mRandom = new Random();
    private final int[] mKeyIds = new int[KEY_FILES.length];
    private int mSpaceId;
    private int mEnterId;
    private int mBackspaceId;
    private SoundPool mPool;
    private int mLoadedProfile = -1;
    private int mLoadCount;
    private boolean mReady;

    public KeySoundPlayer(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Plays one key click. The first call after switching packs only loads the
     * samples, so that one key is silent; everything after it plays normally.
     */
    public void play(int profile, int kind, float volume) {
        if (profile <= PROFILE_SYSTEM || profile >= PROFILE_COUNT) return;

        if (profile != mLoadedProfile) {
            load(profile);
            return;
        }
        if (!mReady || mPool == null) return;

        float level = volume < 0f ? 1f : Math.min(volume, 1f);
        int id;
        switch (kind) {
            case KIND_SPACE:
                id = mSpaceId;
                break;
            case KIND_ENTER:
                id = mEnterId;
                break;
            case KIND_DELETE:
                id = mBackspaceId;
                break;
            default:
                id = mKeyIds[mRandom.nextInt(mKeyIds.length)];
                break;
        }
        if (id != 0) {
            mPool.play(id, level, level, 1, 0, 1.0f);
        }
    }

    public void release() {
        releasePool();
        mLoadedProfile = -1;
    }

    private void releasePool() {
        mReady = false;
        mLoadCount = 0;
        if (mPool != null) {
            mPool.release();
            mPool = null;
        }
    }

    private void load(int profile) {
        releasePool();
        String pack = PACKS[profile];
        if (pack == null) return;
        final String dir = "keysounds/" + pack;

        mLoadedProfile = profile;

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        mPool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attrs)
                .build();
        mPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status != 0) {
                Log.w(TAG, "Sample " + sampleId + " failed to load for " + dir);
                return;
            }
            mLoadCount++;
            if (mLoadCount >= SAMPLE_COUNT) mReady = true;
        });

        for (int i = 0; i < KEY_FILES.length; i++) {
            mKeyIds[i] = loadSample(dir + "/" + KEY_FILES[i]);
        }
        mSpaceId = loadSample(dir + "/space.wav");
        mEnterId = loadSample(dir + "/enter.wav");
        mBackspaceId = loadSample(dir + "/backspace.wav");
    }

    private int loadSample(String assetPath) {
        String cachedName = "keysound_" + assetPath.replace('/', '_');
        File cached = new File(mContext.getCacheDir(), cachedName);
        if (!cached.exists() || cached.length() == 0) {
            try (InputStream in = mContext.getAssets().open(assetPath);
                 OutputStream out = new FileOutputStream(cached)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
            } catch (IOException e) {
                Log.w(TAG, "Missing key sound asset " + assetPath, e);
                return 0;
            }
        }
        return mPool.load(cached.getAbsolutePath(), 1);
    }
}
