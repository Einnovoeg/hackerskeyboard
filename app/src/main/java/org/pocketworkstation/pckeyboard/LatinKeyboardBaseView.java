/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.LinearGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Region.Op;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.StateListDrawable;

import org.pocketworkstation.pckeyboard.Keyboard.Key;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A view that renders a virtual {@link LatinKeyboard}. It handles rendering of keys and
 * detecting key presses and touch movements.
 *
 * TODO: References to LatinKeyboard in this class should be replaced with ones to its base class.
 *
 * @attr ref R.styleable#LatinKeyboardBaseView_keyBackground
 * @attr ref R.styleable#LatinKeyboardBaseView_keyPreviewLayout
 * @attr ref R.styleable#LatinKeyboardBaseView_keyPreviewOffset
 * @attr ref R.styleable#LatinKeyboardBaseView_labelTextSize
 * @attr ref R.styleable#LatinKeyboardBaseView_keyTextSize
 * @attr ref R.styleable#LatinKeyboardBaseView_keyTextColor
 * @attr ref R.styleable#LatinKeyboardBaseView_verticalCorrection
 * @attr ref R.styleable#LatinKeyboardBaseView_popupLayout
 */
public class LatinKeyboardBaseView extends View implements PointerTracker.UIProxy {
    private static final String TAG = "HK/LatinKbdBaseView";
    private static final boolean DEBUG = false;

    public static final int NOT_A_TOUCH_COORDINATE = -1;

    public interface OnKeyboardActionListener {

        /**
         * Called when the user presses a key. This is sent before the
         * {@link #onKey} is called. For keys that repeat, this is only
         * called once.
         *
         * @param primaryCode
         *            the unicode of the key being pressed. If the touch is
         *            not on a valid key, the value will be zero.
         */
        void onPress(int primaryCode);

        /**
         * Called when the user releases a key. This is sent after the
         * {@link #onKey} is called. For keys that repeat, this is only
         * called once.
         *
         * @param primaryCode
         *            the code of the key that was released
         */
        void onRelease(int primaryCode);

        /**
         * Send a key press to the listener.
         *
         * @param primaryCode
         *            this is the key that was pressed
         * @param keyCodes
         *            the codes for all the possible alternative keys with
         *            the primary code being the first. If the primary key
         *            code is a single character such as an alphabet or
         *            number or symbol, the alternatives will include other
         *            characters that may be on the same key or adjacent
         *            keys. These codes are useful to correct for
         *            accidental presses of a key adjacent to the intended
         *            key.
         * @param x
         *            x-coordinate pixel of touched event. If onKey is not called by onTouchEvent,
         *            the value should be NOT_A_TOUCH_COORDINATE.
         * @param y
         *            y-coordinate pixel of touched event. If onKey is not called by onTouchEvent,
         *            the value should be NOT_A_TOUCH_COORDINATE.
         */
        void onKey(int primaryCode, int[] keyCodes, int x, int y);

        /**
         * Sends a sequence of characters to the listener.
         *
         * @param text
         *            the sequence of characters to be displayed.
         */
        void onText(CharSequence text);

        /**
         * Called when user released a finger outside any key.
         */
        void onCancel();

        /**
         * Called when the user quickly moves the finger from right to
         * left.
         */
        boolean swipeLeft();

        /**
         * Called when the user quickly moves the finger from left to
         * right.
         */
        boolean swipeRight();

        /**
         * Called when the user quickly moves the finger from up to down.
         */
        boolean swipeDown();

        /**
         * Called when the user quickly moves the finger from down to up.
         */
        boolean swipeUp();
    }

    // Timing constants
    private final int mKeyRepeatInterval;

    // Miscellaneous constants
    /* package */ static final int NOT_A_KEY = -1;
    private static final int NUMBER_HINT_VERTICAL_ADJUSTMENT_PIXEL = -1;

    // XML attribute
    private float mKeyTextSize;
    private float mLabelScale = 1.0f;
    private int mKeyTextColor;
    private int mKeyHintColor;
    private int mKeyCursorColor;
    private boolean mInvertSymbols;
    private boolean mRecolorSymbols;
    private Typeface mKeyTextStyle = Typeface.DEFAULT;

    /** Label typefaces offered by the "Key font" setting; index 0 keeps the theme default. */
    private static final Typeface[] LABEL_TYPEFACES = new Typeface[] {
            Typeface.DEFAULT,
            Typeface.create("sans-serif", Typeface.NORMAL),
            Typeface.create("sans-serif-condensed", Typeface.NORMAL),
            Typeface.create("sans-serif-light", Typeface.NORMAL),
            Typeface.create("sans-serif-medium", Typeface.NORMAL),
            Typeface.create("sans-serif-black", Typeface.NORMAL),
            Typeface.create("monospace", Typeface.NORMAL),
            Typeface.create("serif", Typeface.NORMAL),
            Typeface.create("casual", Typeface.NORMAL),
            Typeface.create("cursive", Typeface.NORMAL),
    };

    private Typeface mLabelTypefaceHint = Typeface.DEFAULT_BOLD;

    /** Colours offered by the "Key letter colour" setting; index 0 means the theme colour. */
    private static final int[] KEY_TEXT_COLORS = new int[] {
            0, 0xFFFFFFFF, 0xFF000000, 0xFF9AA0A6, 0xFFFFC107, 0xFFFF9F0A,
            0xFFFFEB3B, 0xFFC6FF00, 0xFF34C759, 0xFF00BFA5, 0xFF32ADE6,
            0xFF0A84FF, 0xFF5E5CE6, 0xFFBF5AF2, 0xFFFF6FA5, 0xFFFF453A,
    };

    /** Animated background effects, see key_effect_values in strings.xml. */
    public static final int EFFECT_NONE = 0;
    public static final int EFFECT_MATRIX = 1;
    public static final int EFFECT_STARS = 2;
    public static final int EFFECT_AURORA = 3;
    public static final int EFFECT_SCANLINES = 4;
    public static final int EFFECT_RAIN = 5;
    public static final int EFFECT_BUBBLES = 6;
    public static final int EFFECT_GRID = 7;
    public static final int EFFECT_FIREFLIES = 8;
    public static final int EFFECT_PLASMA = 9;
    public static final int EFFECT_SNOW = 10;
    public static final int EFFECT_VORTEX = 11;
    public static final int EFFECT_RIPPLES = 12;
    public static final int EFFECT_BEAMS = 13;
    public static final int EFFECT_FLOW = 14;
    public static final int EFFECT_CONSTELLATION = 15;

    private static final char[] MATRIX_ALPHABET =
            "01010101ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉ<>[]{}/\\=+*#$%&@".toCharArray();

    private final Paint mMatrixPaint = new Paint();
    private float[] mMatrixHead;
    private float[] mMatrixSpeed;
    private char[] mMatrixGlyph;
    private long mMatrixLastFrame;
    private int mMatrixColumns;

    private float[] mStarX;
    private float[] mStarY;
    private float[] mStarSpeed;
    private long mEffectClock;
    private final Paint mEffectPaint = new Paint();

    private int mKeyBorderColor = 0;
    private final Paint mKeyBorderPaint = new Paint();
    private final Paint mKeyShadowPaint = new Paint();
    private final Paint mKeyBevelPaint = new Paint();
    private final Paint mKeyGlossPaint = new Paint();
    private Shader mKeyGlossShader;
    private int mKeyGlossHeight = -1;
    private final RectF mKeyRect = new RectF();
    private float mKeyCornerRadius;
    private float mKeyShadowDy;
    private float mShadowScale = 1f;
    private float mKeyOpacity = 1f;

    /** Key cap solidity, 0.4-1.0; background art shows through lower values. */
    public void applyKeyOpacity(float opacity) {
        mKeyOpacity = Math.max(0.4f, Math.min(1f, opacity));
        updateKeyCapAlpha();
        invalidate();
    }

    /** Drop-shadow depth multiplier, 0 flattens the caps. */
    public void applyKeyShadow(float scale) {
        mShadowScale = Math.max(0f, Math.min(2f, scale));
        mKeyShadowDy = 2 * getResources().getDisplayMetrics().density * mShadowScale;
        invalidate();
    }

    private void updateKeyCapAlpha() {
        if (mKeyBackground == null) return;
        int base = mBackgroundImage != null ? 205 : 255;
        mKeyBackground.setAlpha(Math.round(base * mKeyOpacity));
    }
    private float mLabelTextSize;
    private int mSymbolColorScheme = 0;
    private int mShadowColor;
    private float mShadowRadius;
    private Drawable mKeyBackground;
    private float mHeightScale = 1f;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        sLastInstance = this;
    }
    private int mKeyRadiusPx = -1;
    private float mBackgroundDim = 0f;
    private Drawable mBackgroundImage;
    private int mBackgroundIndex = -1;
    private float mPinchStartSpan = 0f;
    private float mPinchStartScale = 1f;
    private boolean mPinching = false;
    private Drawable mThemeBackground;
    private boolean mThemeBackgroundCaptured;
    private int mBackgroundAlpha;
    private float mBackgroundDimAmount;
    private float mKeyHysteresisDistance;
    private float mVerticalCorrection;
    protected int mPreviewOffset;
    protected int mPreviewHeight;
    protected int mPopupLayout;

    // Main keyboard
    private Keyboard mKeyboard;
    /** Static rendering for the settings preview: no animation loop, no touch. */
    private boolean mPreviewMode;
    private Key[] mKeys;
    // TODO this attribute should be gotten from Keyboard.
    private int mKeyboardVerticalGap;

    // Key preview popup
    protected TextView mPreviewText;
    protected PopupWindow mPreviewPopup;
    protected int mPreviewTextSizeLarge;
    protected int[] mOffsetInWindow;
    protected int mOldPreviewKeyIndex = NOT_A_KEY;
    protected boolean mShowPreview = true;
    protected boolean mShowTouchPoints = true;
    protected int mPopupPreviewOffsetX;
    protected int mPopupPreviewOffsetY;
    protected int mWindowY;
    protected int mPopupPreviewDisplayedY;
    protected final int mDelayBeforePreview;
    protected final int mDelayBeforeSpacePreview;
    protected final int mDelayAfterPreview;

    // Popup mini keyboard
    protected PopupWindow mMiniKeyboardPopup;
    protected LatinKeyboardBaseView mMiniKeyboard;
    protected View mMiniKeyboardContainer;
    protected View mMiniKeyboardParent;
    protected boolean mMiniKeyboardVisible;
    protected final WeakHashMap<Key, Keyboard> mMiniKeyboardCacheMain = new WeakHashMap<Key, Keyboard>();
    protected final WeakHashMap<Key, Keyboard> mMiniKeyboardCacheShift = new WeakHashMap<Key, Keyboard>();
    protected final WeakHashMap<Key, Keyboard> mMiniKeyboardCacheCaps = new WeakHashMap<Key, Keyboard>();
    protected int mMiniKeyboardOriginX;
    protected int mMiniKeyboardOriginY;
    protected long mMiniKeyboardPopupTime;
    protected int[] mWindowOffset;
    protected final float mMiniKeyboardSlideAllowance;
    protected int mMiniKeyboardTrackerId;

    /** Listener for {@link OnKeyboardActionListener}. */
    private OnKeyboardActionListener mKeyboardActionListener;

    private final ArrayList<PointerTracker> mPointerTrackers = new ArrayList<PointerTracker>();
    private boolean mIgnoreMove = false;

    // TODO: Let the PointerTracker class manage this pointer queue
    private final PointerQueue mPointerQueue = new PointerQueue();

    private final boolean mHasDistinctMultitouch;
    private int mOldPointerCount = 1;

    protected KeyDetector mKeyDetector = new ProximityKeyDetector();

    // Swipe gesture detector
    private GestureDetector mGestureDetector;
    private final SwipeTracker mSwipeTracker = new SwipeTracker();
    private final int mSwipeThreshold;
    private final boolean mDisambiguateSwipe;

    // Drawing
    /** Whether the keyboard bitmap needs to be redrawn before it's blitted. **/
    private boolean mDrawPending;
    /** The dirty region in the keyboard bitmap */
    private final Rect mDirtyRect = new Rect();
    /** The keyboard bitmap for faster updates */
    private Bitmap mBuffer;
    /** Recently pressed keys, for press-effect and reactive RGB animation. */
    private final HashMap<Key, Long> mKeyPressTimes = new HashMap<Key, Long>();
    private static final long KEY_PRESS_EFFECT_MS = 700L;
    private final Paint mPressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mRgbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mPressRect = new RectF();
    /** Notes if the keyboard just changed, so that we could possibly reallocate the mBuffer. */
    private boolean mKeyboardChanged;
    private Key mInvalidatedKey;
    /** The canvas for the above mutable keyboard bitmap */
    private Canvas mCanvas;
    private final Paint mPaint;
    private final Paint mPaintHint;
    private final Rect mPadding;
    private final Rect mClipRegion = new Rect(0, 0, 0, 0);
    private int mViewWidth;
    // This map caches key label text height in pixel as value and key label text size as map key.
    private final HashMap<Integer, Integer> mTextHeightCache = new HashMap<Integer, Integer>();
    // Distance from horizontal center of the key, proportional to key label text height.
    private final float KEY_LABEL_VERTICAL_ADJUSTMENT_FACTOR = 0.55f;
    private final String KEY_LABEL_HEIGHT_REFERENCE_CHAR = "H";
    /* package */ static Method sSetRenderMode;
    private static int sPrevRenderMode = -1;

    private static final float[] INVERTING_MATRIX = {
            -1.f, 0, 0, 0, 255, // Red
            0, -1.f, 0, 0, 255, // Green
            0, 0, -1.f, 0, 255, // Blue
            0, 0, 0, 1.f, 0, // Alpha
    };
    private final ColorMatrixColorFilter mInvertingColorFilter = new ColorMatrixColorFilter(INVERTING_MATRIX);

    private final UIHandler mHandler = new UIHandler();

    class UIHandler extends Handler {
        private static final int MSG_POPUP_PREVIEW = 1;
        private static final int MSG_DISMISS_PREVIEW = 2;
        private static final int MSG_REPEAT_KEY = 3;
        private static final int MSG_LONGPRESS_KEY = 4;

        private boolean mInKeyRepeat;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_POPUP_PREVIEW:
                    showKey(msg.arg1, (PointerTracker)msg.obj);
                    break;
                case MSG_DISMISS_PREVIEW:
                    mPreviewPopup.dismiss();
                    break;
                case MSG_REPEAT_KEY: {
                    final PointerTracker tracker = (PointerTracker)msg.obj;
                    tracker.repeatKey(msg.arg1);
                    startKeyRepeatTimer(mKeyRepeatInterval, msg.arg1, tracker);
                    break;
                }
                case MSG_LONGPRESS_KEY: {
                    final PointerTracker tracker = (PointerTracker)msg.obj;
                    openPopupIfRequired(msg.arg1, tracker);
                    break;
                }
            }
        }

        public void popupPreview(long delay, int keyIndex, PointerTracker tracker) {
            removeMessages(MSG_POPUP_PREVIEW);
            if (mPreviewPopup.isShowing() && mPreviewText.getVisibility() == VISIBLE) {
                // Show right away, if it's already visible and finger is moving around
                showKey(keyIndex, tracker);
            } else {
                sendMessageDelayed(obtainMessage(MSG_POPUP_PREVIEW, keyIndex, 0, tracker),
                        delay);
            }
        }

        public void cancelPopupPreview() {
            removeMessages(MSG_POPUP_PREVIEW);
        }

        public void dismissPreview(long delay) {
            if (mPreviewPopup.isShowing()) {
                sendMessageDelayed(obtainMessage(MSG_DISMISS_PREVIEW), delay);
            }
        }

        public void cancelDismissPreview() {
            removeMessages(MSG_DISMISS_PREVIEW);
        }

        public void startKeyRepeatTimer(long delay, int keyIndex, PointerTracker tracker) {
            mInKeyRepeat = true;
            sendMessageDelayed(obtainMessage(MSG_REPEAT_KEY, keyIndex, 0, tracker), delay);
        }

        public void cancelKeyRepeatTimer() {
            mInKeyRepeat = false;
            removeMessages(MSG_REPEAT_KEY);
        }

        public boolean isInKeyRepeat() {
            return mInKeyRepeat;
        }

        public void startLongPressTimer(long delay, int keyIndex, PointerTracker tracker) {
            removeMessages(MSG_LONGPRESS_KEY);
            sendMessageDelayed(obtainMessage(MSG_LONGPRESS_KEY, keyIndex, 0, tracker), delay);
        }

        public void cancelLongPressTimer() {
            removeMessages(MSG_LONGPRESS_KEY);
        }

        public void cancelKeyTimers() {
            cancelKeyRepeatTimer();
            cancelLongPressTimer();
        }

        public void cancelAllMessages() {
            cancelKeyTimers();
            cancelPopupPreview();
            cancelDismissPreview();
        }
    }

    static class PointerQueue {
        private LinkedList<PointerTracker> mQueue = new LinkedList<PointerTracker>();

        public void add(PointerTracker tracker) {
            mQueue.add(tracker);
        }

        public int lastIndexOf(PointerTracker tracker) {
            LinkedList<PointerTracker> queue = mQueue;
            for (int index = queue.size() - 1; index >= 0; index--) {
                PointerTracker t = queue.get(index);
                if (t == tracker)
                    return index;
            }
            return -1;
        }

        public void releaseAllPointersOlderThan(PointerTracker tracker, long eventTime) {
            LinkedList<PointerTracker> queue = mQueue;
            int oldestPos = 0;
            for (PointerTracker t = queue.get(oldestPos); t != tracker; t = queue.get(oldestPos)) {
                if (t.isModifier()) {
                    oldestPos++;
                } else {
                    t.onUpEvent(t.getLastX(), t.getLastY(), eventTime);
                    t.setAlreadyProcessed();
                    queue.remove(oldestPos);
                }
                if (queue.isEmpty()) return;
            }
        }

        public void releaseAllPointersExcept(PointerTracker tracker, long eventTime) {
            for (PointerTracker t : mQueue) {
                if (t == tracker)
                    continue;
                t.onUpEvent(t.getLastX(), t.getLastY(), eventTime);
                t.setAlreadyProcessed();
            }
            mQueue.clear();
            if (tracker != null)
                mQueue.add(tracker);
        }

        public void remove(PointerTracker tracker) {
            mQueue.remove(tracker);
        }

        public boolean isInSlidingKeyInput() {
            for (final PointerTracker tracker : mQueue) {
                if (tracker.isInSlidingKeyInput())
                    return true;
            }
            return false;
        }
    }

    static {
        initCompatibility();
    }
    
    static void initCompatibility() {
        try {
            sSetRenderMode = View.class.getMethod("setLayerType", int.class, Paint.class);
            Log.i(TAG, "setRenderMode is supported");
        } catch (SecurityException e) {
            Log.w(TAG, "unexpected SecurityException", e);
        } catch (NoSuchMethodException e) {
            // ignore, not supported by API level pre-Honeycomb
            Log.i(TAG, "ignoring render mode, not supported");
        }
    }
    
    private void setRenderModeIfPossible(int mode) {
        if (sSetRenderMode != null && mode != sPrevRenderMode) {
            try {
                sSetRenderMode.invoke(this, mode, null);
                sPrevRenderMode = mode;
                Log.i(TAG, "render mode set to " + LatinIME.sKeyboardSettings.renderMode);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
    
    public LatinKeyboardBaseView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.keyboardViewStyle);
    }

    public LatinKeyboardBaseView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (!isInEditMode())
            Log.i(TAG, "Creating new LatinKeyboardBaseView " + this);
        setRenderModeIfPossible(LatinIME.sKeyboardSettings.renderMode);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.LatinKeyboardBaseView, defStyle, R.style.LatinKeyboardBaseView);

        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);

            // Resource ids are not compile-time constants with modern Android Gradle
            // Plugin versions, so this is an if/else chain instead of a switch.
            if (attr == R.styleable.LatinKeyboardBaseView_keyBackground) {
                mKeyBackground = a.getDrawable(attr);
            } else if (attr == R.styleable.LatinKeyboardBaseView_keyHysteresisDistance) {
                mKeyHysteresisDistance = a.getDimensionPixelOffset(attr, 0);
            } else if (attr == R.styleable.LatinKeyboardBaseView_verticalCorrection) {
                mVerticalCorrection = a.getDimensionPixelOffset(attr, 0);
            } else if (attr == R.styleable.LatinKeyboardBaseView_keyTextSize) {
                mKeyTextSize = a.getDimensionPixelSize(attr, 18);
            } else if (attr == R.styleable.LatinKeyboardBaseView_keyTextColor) {
                mKeyTextColor = a.getColor(attr, 0xFF000000);
            } else if (attr == R.styleable.LatinKeyboardBaseView_keyHintColor) {
                mKeyHintColor = a.getColor(attr, 0xFFBBBBBB);
            } else if (attr == R.styleable.LatinKeyboardBaseView_keyCursorColor) {
                mKeyCursorColor = a.getColor(attr, 0xFF000000);
            } else if (attr == R.styleable.LatinKeyboardBaseView_invertSymbols) {
                mInvertSymbols = a.getBoolean(attr, false);
            } else if (attr == R.styleable.LatinKeyboardBaseView_recolorSymbols) {
                mRecolorSymbols = a.getBoolean(attr, false);
            } else if (attr == R.styleable.LatinKeyboardBaseView_labelTextSize) {
                mLabelTextSize = a.getDimensionPixelSize(attr, 14);
            } else if (attr == R.styleable.LatinKeyboardBaseView_shadowColor) {
                mShadowColor = a.getColor(attr, 0);
            } else if (attr == R.styleable.LatinKeyboardBaseView_shadowRadius) {
                mShadowRadius = a.getFloat(attr, 0f);
            } else if (attr == R.styleable.LatinKeyboardBaseView_backgroundDimAmount) {
                // TODO: Use Theme (android.R.styleable.Theme_backgroundDimAmount)
                mBackgroundDimAmount = a.getFloat(attr, 0.5f);
            } else if (attr == R.styleable.LatinKeyboardBaseView_backgroundAlpha) {
                mBackgroundAlpha = a.getInteger(attr, 255);
            } else if (attr == R.styleable.LatinKeyboardBaseView_keyTextStyle) {
                int textStyle = a.getInt(attr, 0);
                if (textStyle == 0) {
                    mKeyTextStyle = Typeface.DEFAULT;
                } else if (textStyle == 1) {
                    mKeyTextStyle = Typeface.DEFAULT_BOLD;
                } else {
                    mKeyTextStyle = Typeface.defaultFromStyle(textStyle);
                }
            } else if (attr == R.styleable.LatinKeyboardBaseView_keyBorderColor) {
                mKeyBorderColor = a.getColor(attr, 0);
            } else if (attr == R.styleable.LatinKeyboardBaseView_symbolColorScheme) {
                mSymbolColorScheme = a.getInt(attr, 0);
            }
        }

        final Resources res = getResources();

        mShowPreview = false;
        mDelayBeforePreview = res.getInteger(R.integer.config_delay_before_preview);
        mDelayBeforeSpacePreview = res.getInteger(R.integer.config_delay_before_space_preview);
        mDelayAfterPreview = res.getInteger(R.integer.config_delay_after_preview);

        mPopupLayout = 0;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setAlpha(255);

        mPaintHint = new Paint();
        mPaintHint.setAntiAlias(true);
        mPaintHint.setTextAlign(Align.RIGHT);
        mPaintHint.setAlpha(255);
        mPaintHint.setTypeface(Typeface.DEFAULT_BOLD);
        mLabelTypefaceHint = Typeface.DEFAULT_BOLD;

        mPadding = new Rect(0, 0, 0, 0);
        mKeyBackground.getPadding(mPadding);

        final float density = res.getDisplayMetrics().density;
        mKeyCornerRadius = 6 * density;
        mKeyShadowDy = 2 * density;
        mKeyBorderPaint.setStyle(Paint.Style.STROKE);
        mKeyBorderPaint.setAntiAlias(true);
        mKeyBorderPaint.setStrokeWidth(Math.max(1f, density));
        mKeyShadowPaint.setStyle(Paint.Style.FILL);
        mKeyShadowPaint.setAntiAlias(true);
        mKeyShadowPaint.setColor(0x42000000);
        mKeyBevelPaint.setStyle(Paint.Style.STROKE);
        mKeyBevelPaint.setAntiAlias(true);
        mKeyBevelPaint.setStrokeWidth(Math.max(1f, density));
        mKeyGlossPaint.setStyle(Paint.Style.FILL);
        mKeyGlossPaint.setAntiAlias(true);
        mMatrixPaint.setAntiAlias(true);
        mMatrixPaint.setTypeface(Typeface.MONOSPACE);
        mMatrixPaint.setTextAlign(Paint.Align.CENTER);

        mSwipeThreshold = (int) (300 * res.getDisplayMetrics().density);
        // TODO: Refer frameworks/base/core/res/res/values/config.xml
        // TODO(klausw): turn off mDisambiguateSwipe if no swipe actions are set?
        mDisambiguateSwipe = res.getBoolean(R.bool.config_swipeDisambiguation);
        mMiniKeyboardSlideAllowance = res.getDimension(R.dimen.mini_keyboard_slide_allowance);

        GestureDetector.SimpleOnGestureListener listener =
                new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent me1, MotionEvent me2, float velocityX,
                    float velocityY) {
                final float absX = Math.abs(velocityX);
                final float absY = Math.abs(velocityY);
                float deltaX = me2.getX() - me1.getX();
                float deltaY = me2.getY() - me1.getY();
                mSwipeTracker.computeCurrentVelocity(1000);
                final float endingVelocityX = mSwipeTracker.getXVelocity();
                final float endingVelocityY = mSwipeTracker.getYVelocity();
                // Calculate swipe distance threshold based on screen width & height,
                // taking the smaller distance.
                int travelX = getWidth() / 3;
                int travelY = getHeight() / 3;
                int travelMin = Math.min(travelX, travelY);
//                Log.i(TAG, "onFling vX=" + velocityX + " vY=" + velocityY + " threshold=" + mSwipeThreshold
//                        + " dX=" + deltaX + " dy=" + deltaY + " min=" + travelMin);
                if (velocityX > mSwipeThreshold && absY < absX && deltaX > travelMin) {
                    if (mDisambiguateSwipe && endingVelocityX >= velocityX / 4) {
                        if (swipeRight()) return true;
                    }
                } else if (velocityX < -mSwipeThreshold && absY < absX && deltaX < -travelMin) {
                    if (mDisambiguateSwipe && endingVelocityX <= velocityX / 4) {
                        if (swipeLeft()) return true;
                    }
                } else if (velocityY < -mSwipeThreshold && absX < absY && deltaY < -travelMin) {
                    if (mDisambiguateSwipe && endingVelocityY <= velocityY / 4) {
                        if (swipeUp()) return true;
                    }
                } else if (velocityY > mSwipeThreshold && absX < absY / 2 && deltaY > travelMin) {
                    if (mDisambiguateSwipe && endingVelocityY >= velocityY / 4) {
                        if (swipeDown()) return true;
                    }
                }
                return false;
            }
        };

        final boolean ignoreMultitouch = true;
        mGestureDetector = new GestureDetector(getContext(), listener, null, ignoreMultitouch);
        mGestureDetector.setIsLongpressEnabled(false);

        mHasDistinctMultitouch = context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
        mKeyRepeatInterval = res.getInteger(R.integer.config_key_repeat_interval);
    }

    private boolean showHints7Bit() {
        return LatinIME.sKeyboardSettings.hintMode >= 1;
    }

    private boolean showHintsAll() {
        return LatinIME.sKeyboardSettings.hintMode >= 2;
    }

    public void setOnKeyboardActionListener(OnKeyboardActionListener listener) {
        mKeyboardActionListener = listener;
        for (PointerTracker tracker : mPointerTrackers) {
            tracker.setOnKeyboardActionListener(listener);
        }
    }

    /**
     * Returns the {@link OnKeyboardActionListener} object.
     * @return the listener attached to this keyboard
     */
    protected OnKeyboardActionListener getOnKeyboardActionListener() {
        return mKeyboardActionListener;
    }

    /**
     * Attaches a keyboard to this view. The keyboard can be switched at any time and the
     * view will re-layout itself to accommodate the keyboard.
     * @see Keyboard
     * @see #getKeyboard()
     * @param keyboard the keyboard to display in this view
     */
    public void setPreviewMode(boolean preview) {
        mPreviewMode = preview;
        invalidateAllKeys();
    }

    public void setKeyboard(Keyboard keyboard) {
        if (mKeyboard != null) {
            dismissKeyPreview();
        }
        //Log.i(TAG, "setKeyboard(" + keyboard + ") for " + this);
        // Remove any pending messages, except dismissing preview
        mHandler.cancelKeyTimers();
        mHandler.cancelPopupPreview();
        mKeyboard = keyboard;
        mKeyPressTimes.clear();
        // Disable correctionX and correctionY, it doesn't seem to work as intended.
        // mKeys = mKeyDetector.setKeyboard(keyboard, -getPaddingLeft(),-getPaddingTop() + mVerticalCorrection);
        mKeys = mKeyDetector.setKeyboard(keyboard, 0, 0);
        mKeyboardVerticalGap = (int)getResources().getDimension(R.dimen.key_bottom_gap);
        for (PointerTracker tracker : mPointerTrackers) {
            tracker.setKeyboard(mKeys, mKeyHysteresisDistance);
        }
        mLabelScale = LatinIME.sKeyboardSettings.labelScalePref;
        //if (keyboard.mLayoutRows >= 4) mLabelScale *= 5.0f / keyboard.mLayoutRows;
        requestLayout();
        // Hint to reallocate the buffer if the size changed
        mKeyboardChanged = true;
        invalidateAllKeys();
        computeProximityThreshold(keyboard);
        mMiniKeyboardCacheMain.clear();
        mMiniKeyboardCacheShift.clear();
        mMiniKeyboardCacheCaps.clear();
        setRenderModeIfPossible(LatinIME.sKeyboardSettings.renderMode);
        mIgnoreMove = true;
    }
    
    /**
     * Returns the current keyboard being displayed by this view.
     * @return the currently attached keyboard
     * @see #setKeyboard(Keyboard)
     */
    public Keyboard getKeyboard() {
        return mKeyboard;
    }

    /**
     * Narrows the key grid for one-handed mode. Padding decides which side the
     * keys are aligned to; the view itself still fills the IME window so the
     * background art covers the empty side too.
     */
    public void setHorizontalScale(float scale) {
        if (mKeyboard == null) return;
        mKeyboard.setHorizontalScale(scale);
        // Touch coordinates are view coordinates, so compensate for the empty
        // side's padding before asking the keyboard which key was hit.
        mKeys = mKeyDetector.setKeyboard(mKeyboard, -getPaddingLeft(), 0);
        for (PointerTracker tracker : mPointerTrackers) {
            tracker.setKeyboard(mKeys, mKeyHysteresisDistance);
        }
        computeProximityThreshold(mKeyboard);
        requestLayout();
        invalidateAllKeys();
    }

    /**
     * Return whether the device has distinct multi-touch panel.
     * @return true if the device has distinct multi-touch panel.
     */
    public boolean hasDistinctMultitouch() {
        return mHasDistinctMultitouch;
    }

    @Override
    public void onKeyPressed(Key key) {
        if (key == null || mPreviewMode || LatinIME.sKeyboardSettings == null) return;
        if (LatinIME.sKeyboardSettings.keyPressEffect == 0
                && LatinIME.sKeyboardSettings.keyRgbMode != GlobalKeyboardSettings.RGB_REACTIVE) {
            return;
        }
        mKeyPressTimes.put(key, SystemClock.uptimeMillis());
        invalidateAllKeys();
        postInvalidateOnAnimation();
    }

    /**
     * Sets the state of the shift key of the keyboard, if any.
     * @param shiftState whether or not to enable the state of the shift key
     * @return true if the shift key state changed, false if there was no change
     */
    public boolean setShiftState(int shiftState) {
        //Log.i(TAG, "setShifted " + shiftState);
        if (mKeyboard != null) {
            if (mKeyboard.setShiftState(shiftState)) {
                // The whole keyboard probably needs to be redrawn
                invalidateAllKeys();
                return true;
            }
        }
        return false;
    }

    public void setCtrlIndicator(boolean active) {
        if (mKeyboard != null) {
            invalidateKey(mKeyboard.setCtrlIndicator(active));
        }
    }
    
    public void setAltIndicator(boolean active) {
        if (mKeyboard != null) {
            invalidateKey(mKeyboard.setAltIndicator(active));
        }
    }

    public void setMetaIndicator(boolean active) {
        if (mKeyboard != null) {
            invalidateKey(mKeyboard.setMetaIndicator(active));
        }
    }

    /**
     * Returns the state of the shift key of the keyboard, if any.
     * @return true if the shift is in a pressed state, false otherwise. If there is
     * no shift key on the keyboard or there is no keyboard attached, it returns false.
     */
    public int getShiftState() {
        if (mKeyboard != null) {
            return mKeyboard.getShiftState();
        }
        return Keyboard.SHIFT_OFF;
    }
    
    public boolean isShiftCaps() {
        return getShiftState() != Keyboard.SHIFT_OFF;
    }

    public boolean isShiftAll() {
        int state = getShiftState();
        if (LatinIME.sKeyboardSettings.shiftLockModifiers) {
            return state == Keyboard.SHIFT_ON || state == Keyboard.SHIFT_LOCKED;
        } else {
            return state == Keyboard.SHIFT_ON;            
        }
    }

    /**
     * Enables or disables the key feedback popup. This is a popup that shows a magnified
     * version of the depressed key. By default the preview is enabled.
     * @param previewEnabled whether or not to enable the key feedback popup
     * @see #isPreviewEnabled()
     */
    public void setPreviewEnabled(boolean previewEnabled) {
        mShowPreview = previewEnabled;
    }

    /**
     * Returns the enabled state of the key feedback popup.
     * @return whether or not the key feedback popup is enabled
     * @see #setPreviewEnabled(boolean)
     */
    public boolean isPreviewEnabled() {
        return mShowPreview;
    }

    private boolean isBlackSym() {
        return mSymbolColorScheme == 1;
    }

    public void setPopupParent(View v) {
        mMiniKeyboardParent = v;
    }

    public void setPopupOffset(int x, int y) {
        mPopupPreviewOffsetX = x;
        mPopupPreviewOffsetY = y;
        if (mPreviewPopup != null) mPreviewPopup.dismiss();
    }

    /**
     * When enabled, calls to {@link OnKeyboardActionListener#onKey} will include key
     * codes for adjacent keys.  When disabled, only the primary key code will be
     * reported.
     * @param enabled whether or not the proximity correction is enabled
     */
    public void setProximityCorrectionEnabled(boolean enabled) {
        mKeyDetector.setProximityCorrectionEnabled(enabled);
    }

    /**
     * Returns true if proximity correction is enabled.
     */
    public boolean isProximityCorrectionEnabled() {
        return mKeyDetector.isProximityCorrectionEnabled();
    }

    /** Applies user tuning (height, key radius, background dim) to the live keyboard. */
    public void applyCustomization(float heightScale, float radiusDp, float dim) {
        mHeightScale = Math.max(0.6f, Math.min(1.6f, heightScale));
        mBackgroundDim = Math.max(0f, Math.min(0.85f, dim));
        mKeyShadowDy = 2 * getResources().getDisplayMetrics().density * mShadowScale;
        setPivotY(getHeight());
        setScaleY(mHeightScale);
        int radiusPx = Math.round(radiusDp * getResources().getDisplayMetrics().density);
        if (radiusPx != mKeyRadiusPx) {
            mKeyRadiusPx = radiusPx;
            applyKeyRadius(mKeyBackground, radiusPx);
        }
        invalidate();
    }

    /**
     * Paints the chosen art as the view background (the framework always draws it,
     * behind keys and before effects) and makes the key caps translucent so the art
     * reads through the gaps and through the caps themselves.
     */
    private void syncBackgroundImage() {
        if (!mThemeBackgroundCaptured) {
            mThemeBackground = getBackground();
            mThemeBackgroundCaptured = true;
        }
        int index = BackgroundImages.selected(getContext());
        if (index == mBackgroundIndex) return;
        mBackgroundIndex = index;
        int res = BackgroundImages.drawableFor(index);
        if (res == 0) {
            mBackgroundImage = null;
            if (mThemeBackground != null) setBackground(mThemeBackground);
            updateKeyCapAlpha();
        } else {
            mBackgroundImage = getContext().getResources().getDrawable(res, getContext().getTheme());
            setBackground(mBackgroundImage);
            updateKeyCapAlpha();
        }
        invalidate();
    }

    /** Sets the bundled art drawn under the animation effect; null clears it. */
    public void applyBackgroundImage(Drawable drawable) {
        mBackgroundImage = drawable;
        if (drawable != null) {
            drawable.setBounds(0, 0, getWidth(), getHeight());
        }
        invalidate();
    }

    /** Fills the canvas only when no image is behind the effect. */
    private void fillBase(Canvas canvas, int color) {
        if (mBackgroundImage == null) {
            canvas.drawColor(color);
        }
    }

    private void applyKeyRadius(Drawable drawable, int radiusPx) {
        if (drawable == null) return;
        if (drawable instanceof GradientDrawable) {
            ((GradientDrawable) drawable).setCornerRadius(radiusPx);
        } else if (drawable instanceof StateListDrawable) {
            StateListDrawable states = (StateListDrawable) drawable;
            for (int i = 0; i < states.getStateCount(); i++) {
                applyKeyRadius(states.getStateDrawable(i), radiusPx);
            }
        } else if (drawable instanceof LayerDrawable) {
            LayerDrawable layers = (LayerDrawable) drawable;
            for (int i = 0; i < layers.getNumberOfLayers(); i++) {
                applyKeyRadius(layers.getDrawable(i), radiusPx);
            }
        }
    }

    /** Applies a pinch height scale and remembers it for the next input view. */
    public void applyPinchScale(float scale) {
        mHeightScale = Math.max(0.7f, Math.min(1.5f, scale));
        setPivotY(getHeight());
        setScaleY(mHeightScale);
        persistHeightScale();
    }

    private void persistHeightScale() {
        getContext().getSharedPreferences(
                getContext().getPackageName() + "_preferences",
                android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("pref_key_height_scale",
                        String.format(java.util.Locale.US, "%.2f", mHeightScale))
                .apply();
    }

    /** Most recently created keyboard view; used by the debug pinch trigger. */
    public static LatinKeyboardBaseView lastInstance() {
        return sLastInstance;
    }

    private static LatinKeyboardBaseView sLastInstance;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // The settings preview is a picture, never an input surface.
        if (mPreviewMode) return false;
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if (ev.getPointerCount() >= 2) {
                    mPinchStartSpan = Math.abs(ev.getY(0) - ev.getY(1));
                    mPinchStartScale = mHeightScale;
                    mPinching = mPinchStartSpan > 1;
                    if (mPinching) return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mPinching && ev.getPointerCount() >= 2) {
                    float span = Math.abs(ev.getY(0) - ev.getY(1));
                    float scale = Math.max(0.7f, Math.min(1.5f,
                            mPinchStartScale * (span / Math.max(1f, mPinchStartSpan))));
                    applyPinchScale(scale);
                    return true;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mPinching) {
                    mPinching = false;
                    persistHeightScale();
                    return true;
                }
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Round up a little
        if (mKeyboard == null) {
            setMeasuredDimension(
                    getPaddingLeft() + getPaddingRight(), getPaddingTop() + getPaddingBottom());
        } else {
            int width = mKeyboard.getMinWidth() + getPaddingLeft() + getPaddingRight();
            if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
                int badWidth = MeasureSpec.getSize(widthMeasureSpec);
                if (badWidth != width) Log.i(TAG, "ignoring unexpected width=" + badWidth);
            }
            Log.i(TAG, "onMeasure width=" + width);
            int baseHeight = mKeyboard.getHeight() + getPaddingTop() + getPaddingBottom();
            if (mHeightScale != 1f) {
                // Ask the IME window for the scaled height so a pinch never clips keys.
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        Math.round(baseHeight * mHeightScale), MeasureSpec.EXACTLY);
            }
            setMeasuredDimension(width, Math.round(baseHeight * mHeightScale));
        }
    }

    /**
     * Compute the average distance between adjacent keys (horizontally and vertically)
     * and square it to get the proximity threshold. We use a square here and in computing
     * the touch distance from a key's center to avoid taking a square root.
     * @param keyboard
     */
    private void computeProximityThreshold(Keyboard keyboard) {
        if (keyboard == null) return;
        final Key[] keys = mKeys;
        if (keys == null) return;
        int length = keys.length;
        int dimensionSum = 0;
        for (int i = 0; i < length; i++) {
            Key key = keys[i];
            dimensionSum += Math.min(key.width, key.height + mKeyboardVerticalGap) + key.gap;
        }
        if (dimensionSum < 0 || length == 0) return;
        mKeyDetector.setProximityThreshold((int) (dimensionSum * 1.55f / length));
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.i(TAG, "onSizeChanged, w=" + w + ", h=" + h);
        mViewWidth = w;
        // Release the buffer, if any and it will be reallocated on the next draw
        mBuffer = null;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Log.i(TAG, "onDraw called " + canvas.getClipBounds());
        mCanvas = canvas;
        if (mDrawPending || mBuffer == null || mKeyboardChanged) {
            onBufferDraw(canvas);
        }
        if (mBuffer != null) canvas.drawBitmap(mBuffer, 0, 0, null);
    }
    
    private void drawDeadKeyLabel(Canvas canvas, String hint, int x, float baseline, Paint paint) {
        char c = hint.charAt(0);
        String accent = DeadAccentSequence.getSpacing(c);
        canvas.drawText(Keyboard.DEAD_KEY_PLACEHOLDER_STRING, x, baseline, paint);
        canvas.drawText(accent, x, baseline, paint);
    }

    private int getLabelHeight(Paint paint, int labelSize) {
        Integer labelHeightValue = mTextHeightCache.get(labelSize);
        if (labelHeightValue != null) {
            return labelHeightValue;
        } else {
            Rect textBounds = new Rect();
            paint.getTextBounds(KEY_LABEL_HEIGHT_REFERENCE_CHAR, 0, 1, textBounds);
            int labelHeight = textBounds.height();
            mTextHeightCache.put(labelSize, labelHeight);
            return labelHeight;
        }
    }

    private void onBufferDraw(Canvas canvas) {
        //Log.i(TAG, "onBufferDraw called");
        if (/*mBuffer == null ||*/ mKeyboardChanged) {
            mKeyboard.setKeyboardWidth(mViewWidth);
//            if (mBuffer == null || mKeyboardChanged &&
//                    (mBuffer.getWidth() != getWidth() || mBuffer.getHeight() != getHeight())) {
//                // Make sure our bitmap is at least 1x1
//                final int width = Math.max(1, getWidth());
//                final int height = Math.max(1, getHeight());
//                mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//                mCanvas = new Canvas(mBuffer);
//            }
            invalidateAllKeys();
            mKeyboardChanged = false;
        }
        //final Canvas canvas = mCanvas;
        //canvas.clipRect(mDirtyRect, Op.REPLACE);
        canvas.getClipBounds(mDirtyRect);
        //canvas.drawColor(Color.BLACK);

        if (mKeyboard == null) return;

        if (isWoodTheme()) {
            drawWoodGrain(canvas, 0, 0, getWidth(), getHeight(), 64);
        }
        syncBackgroundImage();
        if (isEffectEnabled()) {
            drawBackgroundEffect(canvas);
        }
        if (mBackgroundDim > 0.01f) {
            mEffectPaint.setStyle(Paint.Style.FILL);
            mEffectPaint.setShader(null);
            mEffectPaint.setColor(((int) (mBackgroundDim * 255)) << 24);
            canvas.drawRect(0, 0, getWidth(), getHeight(), mEffectPaint);
        }

        final Paint paint = mPaint;
        final Paint paintHint = mPaintHint;
        final int keyTextColor = getKeyTextColor();
        paintHint.setColor(mKeyHintColor);
        mLabelTypefaceHint = LatinIME.sKeyboardSettings == null || LatinIME.sKeyboardSettings.keyFont <= 0
                ? Typeface.DEFAULT_BOLD
                : getLabelTypeface(false);
        paintHint.setTypeface(mLabelTypefaceHint);
        final Drawable keyBackground = mKeyBackground;
        final Rect clipRegion = mClipRegion;
        final Rect padding = mPadding;
        final int kbdPaddingLeft = getPaddingLeft();
        final int kbdPaddingTop = getPaddingTop();
        final Key[] keys = mKeys;
        final Key invalidKey = mInvalidatedKey;
        final long effectNow = SystemClock.uptimeMillis();

        ColorFilter iconColorFilter = null;
        ColorFilter shadowColorFilter = null;
        if (mInvertSymbols) {
            iconColorFilter = mInvertingColorFilter;
        } else if (mRecolorSymbols) {
            // TODO: cache these?
            iconColorFilter = new PorterDuffColorFilter(
                    keyTextColor, PorterDuff.Mode.SRC_ATOP);
            shadowColorFilter = new PorterDuffColorFilter(
                    mShadowColor, PorterDuff.Mode.SRC_ATOP);
        }

        boolean drawSingleKey = false;
        if (invalidKey != null && canvas.getClipBounds(clipRegion)) {
            // TODO we should use Rect.inset and Rect.contains here.
            // Is clipRegion completely contained within the invalidated key?
            if (invalidKey.x + kbdPaddingLeft - 1 <= clipRegion.left &&
                    invalidKey.y + kbdPaddingTop - 1 <= clipRegion.top &&
                    invalidKey.x + invalidKey.width + kbdPaddingLeft + 1 >= clipRegion.right &&
                    invalidKey.y + invalidKey.height + kbdPaddingTop + 1 >= clipRegion.bottom) {
                drawSingleKey = true;
            }
        }
        //fillBase(canvas, 0x00000000, PorterDuff.Mode.CLEAR);
        final int keyCount = keys.length;

        // Scale the key labels based on the median key size.
        List<Integer> keyWidths = new ArrayList<Integer>();
        List<Integer> keyHeights = new ArrayList<Integer>();
        for (int i = 0; i < keyCount; i++) {
            final Key key = keys[i];
            keyWidths.add(key.width);
            keyHeights.add(key.height);
        }
        Collections.sort(keyWidths);
        Collections.sort(keyHeights);
        int medianKeyWidth = keyWidths.get(keyCount / 2);
        int medianKeyHeight = keyHeights.get(keyCount / 2);
        // Use 60% of the smaller of width or height. This is kind of arbitrary.
        mKeyTextSize = Math.min(medianKeyHeight * 6 / 10, medianKeyWidth * 6 / 10);
        mLabelTextSize = mKeyTextSize * 3 / 4;

        int keysDrawn = 0;
        for (int i = 0; i < keyCount; i++) {
            final Key key = keys[i];
            if (drawSingleKey && invalidKey != key) {
                continue;
            }
            if (!mDirtyRect.intersects(
                    key.x + kbdPaddingLeft,
                    key.y + kbdPaddingTop,
                    key.x + key.width + kbdPaddingLeft,
                    key.y + key.height + kbdPaddingTop)) {
                continue;
            }
            keysDrawn++;
            final int rgbColor = rgbColorFor(key, effectNow);
            int labelColor = key.customTextColor != 0 ? key.customTextColor : keyTextColor;
            if (rgbColor != 0 && key.customTextColor == 0) {
                labelColor = contrastTextColor(rgbColor);
            }
            paint.setColor(key.isCursor ? mKeyCursorColor : labelColor);

            int[] drawableState = key.getCurrentDrawableState();
            keyBackground.setState(drawableState);

            // Switch the character to uppercase if shift is pressed
            String label = applyGlyphStyle(key.getCaseLabel(), key.codes);

            float yscale = 1.0f;
            final Rect bounds = keyBackground.getBounds();
            if (key.width != bounds.right || key.height != bounds.bottom) {
                int minHeight = keyBackground.getMinimumHeight();
                if (minHeight > key.height) {
                    yscale = (float) key.height / minHeight;
                    keyBackground.setBounds(0, 0, key.width, minHeight);
                } else {
                    keyBackground.setBounds(0, 0, key.width, key.height);
                }
            }
            canvas.translate(key.x + kbdPaddingLeft, key.y + kbdPaddingTop);
            if (yscale != 1.0f) {
                canvas.save();
                canvas.scale(1.0f, yscale);
            }
            if (mBackgroundAlpha != 255) {
                keyBackground.setAlpha(mBackgroundAlpha);
            } else if (isEffectEnabled()) {
                // Let the effect show faintly through the caps.
                keyBackground.setAlpha(205);
            }

            final int keyStyle = LatinIME.sKeyboardSettings.keyStyle;
            final boolean drawableKey = key.codes.length > 0 || key.icon != null || key.label != null;
            if (drawableKey && keyStyle == LatinIME.KEY_STYLE_RAISED) {
                // Cast a soft shadow below the cap so the keys read as raised. This is
                // barely visible on dark themes, which is what the bevel below is for.
                mKeyRect.set(1, mKeyShadowDy + 1, key.width - 1, key.height + mKeyShadowDy);
                canvas.drawRoundRect(mKeyRect, mKeyCornerRadius, mKeyCornerRadius, mKeyShadowPaint);
            }
            keyBackground.draw(canvas);
            if (rgbColor != 0) {
                mRgbPaint.setStyle(Paint.Style.FILL);
                mRgbPaint.setShader(null);
                mRgbPaint.setColor(rgbColor);
                mPressRect.set(0, 0, key.width, key.height);
                canvas.drawRoundRect(mPressRect, mKeyCornerRadius, mKeyCornerRadius, mRgbPaint);
            }
            if (drawableKey && isWoodTheme()) {
                canvas.save();
                canvas.clipRect(0, 0, key.width, key.height);
                drawWoodGrain(canvas, 0, 0, key.width, key.height, 40);
                canvas.restore();
            }
            if (drawableKey && keyStyle != LatinIME.KEY_STYLE_FLAT && mKeyBorderColor != 0) {
                final float inset = mKeyBorderPaint.getStrokeWidth() / 2f;
                mKeyRect.set(inset, inset, key.width - inset, key.height - inset);
                mKeyBorderPaint.setColor(mKeyBorderColor);
                canvas.drawRoundRect(mKeyRect, mKeyCornerRadius, mKeyCornerRadius, mKeyBorderPaint);

                if (keyStyle == LatinIME.KEY_STYLE_RAISED) {
                    // Bevel: light along the top edge, shaded along the bottom one, so
                    // the cap looks like a physical key on dark and light themes alike.
                    final int pad = Math.round(inset + 2 * getResources().getDisplayMetrics().density);
                    final int right = key.width - pad;
                    if (right > pad) {
                        mKeyBevelPaint.setColor(0x66FFFFFF);
                        canvas.drawLine(pad, inset + 1, right, inset + 1, mKeyBevelPaint);
                        mKeyBevelPaint.setColor(0x66000000);
                        canvas.drawLine(pad, key.height - inset - 1, right, key.height - inset - 1,
                                mKeyBevelPaint);
                    }

                    // Light falling on the top of the cap: bright at the top edge,
                    // fading out before the middle of the key.
                    if (mKeyGlossShader == null || mKeyGlossHeight != key.height) {
                        mKeyGlossShader = new LinearGradient(0, 0, 0, key.height * 0.6f,
                                0x2EFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
                        mKeyGlossHeight = key.height;
                    }
                    mKeyGlossPaint.setShader(mKeyGlossShader);
                    canvas.drawRoundRect(mKeyRect, mKeyCornerRadius, mKeyCornerRadius, mKeyGlossPaint);
                    mKeyGlossPaint.setShader(null);
                }
            }
            if (yscale != 1.0f)  canvas.restore();

            boolean shouldDrawIcon = true;
            if (label != null) {
                // For characters, use large font. For labels like "Done", use small font.
                int labelSize;
                final boolean wordLabel = label.length() > 1 && key.codes.length < 2;
                if (wordLabel) {
                    //Log.i(TAG, "mLabelTextSize=" + mLabelTextSize + " LatinIME.sKeyboardSettings.labelScale=" + LatinIME.sKeyboardSettings.labelScale);
                    labelSize = (int)(mLabelTextSize * mLabelScale);
                } else {
                    labelSize = (int)(mKeyTextSize * mLabelScale);
                }
                paint.setTypeface(getLabelTypeface(wordLabel));

                // Keep the label inside its key no matter what scale or font is chosen.
                final int maxByHeight = (int) (key.height * 0.62f);
                final int maxByWidth = label.length() > 1
                        ? (int) (key.width * 1.55f / label.length())
                        : (int) (key.width * 0.85f);
                labelSize = Math.max(8, Math.min(labelSize, Math.min(maxByHeight, maxByWidth)));
                final boolean boldKeys = LatinIME.sKeyboardSettings != null
                        && LatinIME.sKeyboardSettings.keyBold;
                paint.setFakeBoldText(key.isCursor || boldKeys);
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    // Tracking applies to single-character keys only; word labels keep their fit.
                    float tracking = LatinIME.sKeyboardSettings == null ? 0f
                            : Math.max(0f, Math.min(0.2f, LatinIME.sKeyboardSettings.keyTracking));
                    paint.setLetterSpacing(label.length() == 1 ? tracking : 0f);
                }
                paint.setTextSize(labelSize);

                final int labelHeight = getLabelHeight(paint, labelSize);

                // Draw a drop shadow for the text
                paint.setShadowLayer(mShadowRadius, 0, 0, mShadowColor);

                // Draw hint label (if present) behind the main key
                String hint = key.getHintLabel(showHints7Bit(), showHintsAll());
                if (!hint.equals("") && !(key.isShifted() && key.shiftLabel != null && hint.charAt(0) == key.shiftLabel.charAt(0))) {
                    int hintTextSize = (int)(mKeyTextSize * 0.6 * mLabelScale);
                    paintHint.setTextSize(hintTextSize);

                    final int hintLabelHeight = getLabelHeight(paintHint, hintTextSize);
                    int x = key.width - padding.right;
                    int baseline = padding.top + hintLabelHeight * 12/10;
                    if (Character.getType(hint.charAt(0)) == Character.NON_SPACING_MARK) {
                        drawDeadKeyLabel(canvas, hint, x, baseline, paintHint);
                    } else {
                        canvas.drawText(hint, x, baseline, paintHint);
                    }
                }

                // Draw alternate hint label (if present) behind the main key
                String altHint = key.getAltHintLabel(showHints7Bit(), showHintsAll());
                if (!altHint.equals("")) {
                    int hintTextSize = (int)(mKeyTextSize * 0.6 * mLabelScale);
                    paintHint.setTextSize(hintTextSize);

                    final int hintLabelHeight = getLabelHeight(paintHint, hintTextSize);
                    int x = key.width - padding.right;
                    int baseline = padding.top + hintLabelHeight * (hint.equals("") ? 12 : 26)/10;
                    if (Character.getType(altHint.charAt(0)) == Character.NON_SPACING_MARK) {
                        drawDeadKeyLabel(canvas, altHint, x, baseline, paintHint);
                    } else {
                        canvas.drawText(altHint, x, baseline, paintHint);
                    }
                }

                // Draw main key label
                final int centerX = (key.width + padding.left - padding.right) / 2;
                final int centerY = (key.height + padding.top - padding.bottom) / 2;
                final float baseline = centerY
                        + labelHeight * KEY_LABEL_VERTICAL_ADJUSTMENT_FACTOR;
                if (key.isDeadKey()) {
                    drawDeadKeyLabel(canvas, label, centerX, baseline, paint);
                } else {
                    canvas.drawText(label, centerX, baseline, paint);
                }
                if (key.isCursor) {
                    // poor man's bold - FIXME
                    // Turn off drop shadow
                    paint.setShadowLayer(0, 0, 0, 0);

                    canvas.drawText(label, centerX+0.5f, baseline, paint);
                    canvas.drawText(label, centerX-0.5f, baseline, paint);
                    canvas.drawText(label, centerX, baseline+0.5f, paint);
                    canvas.drawText(label, centerX, baseline-0.5f, paint);
                }

                // Turn off drop shadow
                paint.setShadowLayer(0, 0, 0, 0);

                // Usually don't draw icon if label is not null, but we draw icon for the number
                // hint and popup hint.
                shouldDrawIcon = shouldDrawLabelAndIcon(key);
            }
            Drawable icon = key.icon;
            if (icon != null && shouldDrawIcon) {
                // Special handing for the upper-right number hint icons
                final int drawableWidth;
                final int drawableHeight;
                final int drawableX;
                final int drawableY;
                if (shouldDrawIconFully(key)) {
                    drawableWidth = key.width;
                    drawableHeight = key.height;
                    drawableX = 0;
                    drawableY = NUMBER_HINT_VERTICAL_ADJUSTMENT_PIXEL;
                } else {
                    drawableWidth = icon.getIntrinsicWidth();
                    drawableHeight = icon.getIntrinsicHeight();
                    drawableX = (key.width + padding.left - padding.right - drawableWidth) / 2;
                    drawableY = (key.height + padding.top - padding.bottom - drawableHeight) / 2;
                }
                canvas.translate(drawableX, drawableY);
                icon.setBounds(0, 0, drawableWidth, drawableHeight);

                if (iconColorFilter != null) {
                    // Re-color the icon to match the theme, and draw a shadow for it manually.
                    //
                    // This doesn't seem to look quite right, possibly a problem with using
                    // premultiplied icon images?

                    // Try EmbossMaskFilter, and/or offset? Configurable?
                    if (shadowColorFilter != null && mShadowRadius > 0) {
                        BlurMaskFilter shadowBlur = new BlurMaskFilter(mShadowRadius, BlurMaskFilter.Blur.OUTER);
                        Paint blurPaint = new Paint();
                        blurPaint.setMaskFilter(shadowBlur);
                        Bitmap tmpIcon = Bitmap.createBitmap(key.width, key.height, Bitmap.Config.ARGB_8888);
                        Canvas tmpCanvas = new Canvas(tmpIcon);
                        icon.draw(tmpCanvas);
                        int[] offsets = new int[2];
                        Bitmap shadowBitmap = tmpIcon.extractAlpha(blurPaint, offsets);
                        Paint shadowPaint = new Paint();
                        shadowPaint.setColorFilter(shadowColorFilter);
                        canvas.drawBitmap(shadowBitmap, offsets[0], offsets[1], shadowPaint);
                    }
                    icon.setColorFilter(iconColorFilter);
                    icon.draw(canvas);
                    icon.setColorFilter(null);
                } else {
                    icon.draw(canvas);                    
                }
                canvas.translate(-drawableX, -drawableY);
            }
            canvas.translate(-key.x - kbdPaddingLeft, -key.y - kbdPaddingTop);
        }
        //Log.i(TAG, "keysDrawn=" + keysDrawn);
        drawKeyPressEffects(canvas, kbdPaddingLeft, kbdPaddingTop, effectNow);
        mInvalidatedKey = null;
        // Overlay a dark rectangle to dim the keyboard
        if (mMiniKeyboardVisible) {
            paint.setColor((int) (mBackgroundDimAmount * 0xFF) << 24);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        }

        if (LatinIME.sKeyboardSettings.showTouchPos || DEBUG) {
            if (LatinIME.sKeyboardSettings.showTouchPos || mShowTouchPoints) {
                for (PointerTracker tracker : mPointerTrackers) {
                    int startX = tracker.getStartX();
                    int startY = tracker.getStartY();
                    int lastX = tracker.getLastX();
                    int lastY = tracker.getLastY();
                    paint.setAlpha(128);
                    paint.setColor(0xFFFF0000);
                    canvas.drawCircle(startX, startY, 3, paint);
                    canvas.drawLine(startX, startY, lastX, lastY, paint);
                    paint.setColor(0xFF0000FF);
                    canvas.drawCircle(lastX, lastY, 3, paint);
                    paint.setColor(0xFF00FF00);
                    canvas.drawCircle((startX + lastX) / 2, (startY + lastY) / 2, 2, paint);
                }
            }
        }

        mDrawPending = false;
        mDirtyRect.setEmpty();
        if (LatinIME.sKeyboardSettings != null
                && LatinIME.sKeyboardSettings.keyRgbMode != GlobalKeyboardSettings.RGB_OFF
                && !mPreviewMode) {
            postInvalidateOnAnimation();
        }
    }

    // TODO: clean up this method.
    private void dismissKeyPreview() {
        for (PointerTracker tracker : mPointerTrackers)
            tracker.updateKey(NOT_A_KEY);
        //Log.i(TAG, "dismissKeyPreview() for " + this);
        showPreview(NOT_A_KEY, null);
    }

    public void showPreview(int keyIndex, PointerTracker tracker) {
        int oldKeyIndex = mOldPreviewKeyIndex;
        mOldPreviewKeyIndex = keyIndex;
        final boolean isLanguageSwitchEnabled = (mKeyboard instanceof LatinKeyboard)
                && ((LatinKeyboard)mKeyboard).isLanguageSwitchEnabled();
        // We should re-draw popup preview when 1) we need to hide the preview, 2) we will show
        // the space key preview and 3) pointer moves off the space key to other letter key, we
        // should hide the preview of the previous key.
        final boolean hidePreviewOrShowSpaceKeyPreview = (tracker == null)
                || tracker.isSpaceKey(keyIndex) || tracker.isSpaceKey(oldKeyIndex);
        // If key changed and preview is on or the key is space (language switch is enabled)
        if (oldKeyIndex != keyIndex
                && (mShowPreview
                        || (hidePreviewOrShowSpaceKeyPreview && isLanguageSwitchEnabled))) {
            if (keyIndex == NOT_A_KEY) {
                mHandler.cancelPopupPreview();
                mHandler.dismissPreview(mDelayAfterPreview);
            } else if (tracker != null) {
                int delay = mShowPreview ? mDelayBeforePreview : mDelayBeforeSpacePreview;
                mHandler.popupPreview(delay, keyIndex, tracker);
            }
        }
    }

    private void showKey(final int keyIndex, PointerTracker tracker) {
        Key key = tracker.getKey(keyIndex);
        if (key == null)
            return;
        //Log.i(TAG, "showKey() for " + this);
        // Should not draw hint icon in key preview
        Drawable icon = key.icon;
        if (icon != null && !shouldDrawLabelAndIcon(key)) {
            mPreviewText.setCompoundDrawables(null, null, null,
                    key.iconPreview != null ? key.iconPreview : icon);
            mPreviewText.setText(null);
        } else {
            mPreviewText.setCompoundDrawables(null, null, null, null);
            mPreviewText.setText(key.getCaseLabel());
            if (key.label.length() > 1 && key.codes.length < 2) {
                mPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mKeyTextSize);
                mPreviewText.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                mPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mPreviewTextSizeLarge);
                mPreviewText.setTypeface(mKeyTextStyle);
            }
        }
        mPreviewText.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int popupWidth = Math.max(mPreviewText.getMeasuredWidth(), key.width
                + mPreviewText.getPaddingLeft() + mPreviewText.getPaddingRight());
        final int popupHeight = mPreviewHeight;
        LayoutParams lp = mPreviewText.getLayoutParams();
        if (lp != null) {
            lp.width = popupWidth;
            lp.height = popupHeight;
        }

        int popupPreviewX = key.x - (popupWidth - key.width) / 2;
        int popupPreviewY = key.y - popupHeight + mPreviewOffset;

        mHandler.cancelDismissPreview();
        if (mOffsetInWindow == null) {
            mOffsetInWindow = new int[2];
            getLocationInWindow(mOffsetInWindow);
            mOffsetInWindow[0] += mPopupPreviewOffsetX; // Offset may be zero
            mOffsetInWindow[1] += mPopupPreviewOffsetY; // Offset may be zero
            int[] windowLocation = new int[2];
            getLocationOnScreen(windowLocation);
            mWindowY = windowLocation[1];
        }
        // Set the preview background state.
        // Retrieve and cache the popup keyboard if any.
        boolean hasPopup = (getLongPressKeyboard(key) != null);
        // Set background manually, the StateListDrawable doesn't work.
        mPreviewText.setBackgroundDrawable(getResources().getDrawable(hasPopup ? R.drawable.keyboard_key_feedback_more_background : R.drawable.keyboard_key_feedback_background));
        popupPreviewX += mOffsetInWindow[0];
        popupPreviewY += mOffsetInWindow[1];

        // If the popup cannot be shown above the key, put it on the side
        if (popupPreviewY + mWindowY < 0) {
            // If the key you're pressing is on the left side of the keyboard, show the popup on
            // the right, offset by enough to see at least one key to the left/right.
            if (key.x + key.width <= getWidth() / 2) {
                popupPreviewX += (int) (key.width * 2.5);
            } else {
                popupPreviewX -= (int) (key.width * 2.5);
            }
            popupPreviewY += popupHeight;
        }

        if (mPreviewPopup.isShowing()) {
            mPreviewPopup.update(popupPreviewX, popupPreviewY, popupWidth, popupHeight);
        } else {
            mPreviewPopup.setWidth(popupWidth);
            mPreviewPopup.setHeight(popupHeight);
            mPreviewPopup.showAtLocation(mMiniKeyboardParent, Gravity.NO_GRAVITY,
                    popupPreviewX, popupPreviewY);
        }
        // Record popup preview position to display mini-keyboard later at the same positon
        mPopupPreviewDisplayedY = popupPreviewY;
        mPreviewText.setVisibility(VISIBLE);
    }

    /**
     * Requests a redraw of the entire keyboard. Calling {@link #invalidate} is not sufficient
     * because the keyboard renders the keys to an off-screen buffer and an invalidate() only
     * draws the cached buffer.
     * @see #invalidateKey(Key)
     */
    public void invalidateAllKeys() {
        mDirtyRect.union(0, 0, getWidth(), getHeight());
        mDrawPending = true;
        invalidate();
    }

    /**
     * Invalidates a key so that it will be redrawn on the next repaint. Use this method if only
     * one key is changing it's content. Any changes that affect the position or size of the key
     * may not be honored.
     * @param key key in the attached {@link Keyboard}.
     * @see #invalidateAllKeys
     */
    public void invalidateKey(Key key) {
        if (key == null)
            return;
        mInvalidatedKey = key;
        // TODO we should clean up this and record key's region to use in onBufferDraw.
        mDirtyRect.union(key.x + getPaddingLeft(), key.y + getPaddingTop(),
                key.x + key.width + getPaddingLeft(), key.y + key.height + getPaddingTop());
        //onBufferDraw();
        invalidate(key.x + getPaddingLeft(), key.y + getPaddingTop(),
                key.x + key.width + getPaddingLeft(), key.y + key.height + getPaddingTop());
    }

    /** Colour of the ambient RGB lighting for one key at the current frame. */
    private int rgbColorFor(Key key, long now) {
        final GlobalKeyboardSettings settings = LatinIME.sKeyboardSettings;
        if (settings == null || settings.keyRgbMode == GlobalKeyboardSettings.RGB_OFF) return 0;
        final float speed = Math.max(0.2f, Math.min(3f, settings.keyRgbSpeed));
        final float brightness = Math.max(0.15f, Math.min(1f, settings.keyRgbBrightness));
        float hue = settings.keyRgbHue;
        float level = brightness;
        int alpha;
        switch (settings.keyRgbMode) {
            case GlobalKeyboardSettings.RGB_RAINBOW:
                hue += (key.x * 360f / Math.max(1, mViewWidth)) + (now * 0.04f * speed);
                alpha = (int) (35 + 110 * brightness);
                break;
            case GlobalKeyboardSettings.RGB_BREATHING:
                level *= 0.55f + 0.45f * (float) Math.sin(now * 0.0025f * speed);
                alpha = (int) (25 + 135 * level);
                break;
            case GlobalKeyboardSettings.RGB_WAVE:
                hue += key.x * 0.35f + key.y * 0.55f + now * 0.13f * speed;
                alpha = (int) (30 + 120 * brightness);
                break;
            case GlobalKeyboardSettings.RGB_REACTIVE:
                Long started = mKeyPressTimes.get(key);
                if (started == null) return 0;
                long age = now - started;
                if (age > 420L) return 0;
                hue += key.x * 0.55f;
                alpha = (int) (70 + 170 * brightness * (1f - age / 420f));
                break;
            case GlobalKeyboardSettings.RGB_STATIC:
            default:
                alpha = (int) (35 + 110 * brightness);
                break;
        }
        hue = ((hue % 360f) + 360f) % 360f;
        return Color.HSVToColor(Math.max(0, Math.min(255, alpha)),
                new float[]{hue, 0.82f, 1f});
    }

    /** Picks black or white so a tinted key label stays readable. */
    private static int contrastTextColor(int color) {
        int luminance = (299 * Color.red(color) + 587 * Color.green(color)
                + 114 * Color.blue(color)) / 1000;
        return luminance > 145 ? 0xFF101010 : 0xFFFFFFFF;
    }

    /** Colour for a press animation, following the selected RGB palette when on. */
    private int pressColor(Key key, int effect, float fade, long now) {
        final GlobalKeyboardSettings settings = LatinIME.sKeyboardSettings;
        if (settings != null && settings.keyRgbMode != GlobalKeyboardSettings.RGB_OFF) {
            float hue = settings.keyRgbHue + key.x * 0.6f + now * 0.05f
                    * Math.max(0.2f, settings.keyRgbSpeed);
            hue = ((hue % 360f) + 360f) % 360f;
            return Color.HSVToColor((int) (220 * fade),
                    new float[]{hue, 0.9f, 1f});
        }
        final int base;
        switch (effect) {
            case 2: base = 0xFF4FC3F7; break;  // ripple
            case 3: base = 0xFFFF4FD8; break;  // neon edge
            case 4: base = 0xFFFFB74D; break;  // sparks
            case 6: base = 0xFF69F0AE; break;  // scanline
            case 7: base = 0xFFE1BEE7; break;  // starlight
            case 8: base = 0xFFFF5252; break;  // shockwave
            default: base = 0xFF80D8FF; break; // glow and rainbow flash
        }
        return Color.argb((int) (220 * fade), Color.red(base), Color.green(base), Color.blue(base));
    }

    /** Draws the selected touch effect over recently pressed keys. */
    private void drawKeyPressEffects(Canvas canvas, int padLeft, int padTop, long now) {
        final GlobalKeyboardSettings settings = LatinIME.sKeyboardSettings;
        if (settings == null) return;
        final int effect = settings.keyPressEffect;
        if (effect == 0 && settings.keyRgbMode != GlobalKeyboardSettings.RGB_REACTIVE) return;
        if (mKeyPressTimes.isEmpty()) return;

        final float density = getResources().getDisplayMetrics().density;
        final Iterator<Map.Entry<Key, Long>> iterator = mKeyPressTimes.entrySet().iterator();
        boolean active = false;
        while (iterator.hasNext()) {
            Map.Entry<Key, Long> entry = iterator.next();
            long age = now - entry.getValue();
            if (age < 0 || age > KEY_PRESS_EFFECT_MS) {
                iterator.remove();
                continue;
            }
            active = true;
            if (effect == 0) continue;
            final Key key = entry.getKey();
            if (key == null) continue;
            final float t = age / (float) KEY_PRESS_EFFECT_MS;
            final float fade = 1f - t;
            final float left = padLeft + key.x;
            final float top = padTop + key.y;
            final float right = left + key.width;
            final float bottom = top + key.height;
            final float cx = (left + right) / 2f;
            final float cy = (top + bottom) / 2f;
            final int color = pressColor(key, effect, fade, now);
            mPressPaint.setShader(null);
            mPressPaint.setShadowLayer(0, 0, 0, 0);

            switch (effect) {
                case 1: { // layered glow
                    mPressPaint.setStyle(Paint.Style.STROKE);
                    mPressRect.set(left + 1, top + 1, right - 1, bottom - 1);
                    final int glowAlpha = Color.alpha(color);
                    for (int layer = 4; layer >= 1; layer--) {
                        mPressPaint.setStrokeWidth((layer * 2.5f + 1f) * density);
                        mPressPaint.setColor(Color.argb(glowAlpha / (layer * 3),
                                Color.red(color), Color.green(color), Color.blue(color)));
                        canvas.drawRoundRect(mPressRect, mKeyCornerRadius, mKeyCornerRadius,
                                mPressPaint);
                    }
                    break;
                }
                case 2: { // ripple
                    mPressPaint.setStyle(Paint.Style.STROKE);
                    mPressPaint.setStrokeWidth(2.2f * density);
                    mPressPaint.setColor(color);
                    float radius = Math.max(key.width, key.height) * (0.25f + 0.7f * t);
                    canvas.drawCircle(cx, cy, radius, mPressPaint);
                    mPressPaint.setAlpha(Math.max(0, Color.alpha(color) / 3));
                    canvas.drawCircle(cx, cy, radius * 0.72f, mPressPaint);
                    break;
                }
                case 3: { // neon edge
                    mPressPaint.setStyle(Paint.Style.STROKE);
                    mPressPaint.setStrokeWidth(2.5f * density);
                    mPressPaint.setColor(color);
                    float inset = 1.5f * density + 3f * density * t;
                    mPressRect.set(left + inset, top + inset, right - inset, bottom - inset);
                    canvas.drawRoundRect(mPressRect, mKeyCornerRadius, mKeyCornerRadius, mPressPaint);
                    mPressPaint.setAlpha(Math.max(0, Color.alpha(color) / 3));
                    mPressRect.inset(-2 * density, -2 * density);
                    canvas.drawRoundRect(mPressRect, mKeyCornerRadius, mKeyCornerRadius, mPressPaint);
                    break;
                }
                case 4: { // spark burst
                    mPressPaint.setStyle(Paint.Style.FILL);
                    final int seed = Math.abs(key.hashCode());
                    for (int i = 0; i < 8; i++) {
                        double angle = (seed % 31 + i * 47) * Math.PI / 180.0;
                        float distance = (0.15f + 0.7f * t) * Math.max(key.width, key.height) * 0.55f;
                        float px = cx + (float) Math.cos(angle) * distance;
                        float py = cy + (float) Math.sin(angle) * distance - 4 * density * t;
                        mPressPaint.setColor(color);
                        mPressPaint.setAlpha(Math.max(0, Color.alpha(color) - (int) (120 * t)));
                        canvas.drawCircle(px, py, Math.max(1f, 2.6f * density * fade), mPressPaint);
                    }
                    break;
                }
                case 5: { // rainbow flash
                    float hue = ((now * 0.35f + key.x * 0.7f) % 360f + 360f) % 360f;
                    mPressPaint.setStyle(Paint.Style.FILL);
                    mPressPaint.setColor(Color.HSVToColor((int) (55 * fade),
                            new float[]{hue, 0.9f, 1f}));
                    mPressRect.set(left + 1, top + 1, right - 1, bottom - 1);
                    canvas.drawRoundRect(mPressRect, mKeyCornerRadius, mKeyCornerRadius, mPressPaint);
                    mPressPaint.setStyle(Paint.Style.STROKE);
                    mPressPaint.setStrokeWidth(2.5f * density);
                    mPressPaint.setColor(Color.HSVToColor((int) (210 * fade),
                            new float[]{hue, 0.9f, 1f}));
                    canvas.drawRoundRect(mPressRect, mKeyCornerRadius, mKeyCornerRadius, mPressPaint);
                    break;
                }
                case 6: { // scanline sweep
                    canvas.save();
                    mPressRect.set(left, top, right, bottom);
                    canvas.clipRect(mPressRect);
                    float y = top + (bottom - top) * t;
                    mPressPaint.setStyle(Paint.Style.FILL);
                    mPressPaint.setColor(color);
                    canvas.drawRect(left, y - 1.5f * density, right, y + 1.5f * density, mPressPaint);
                    mPressPaint.setAlpha(Math.max(0, Color.alpha(color) / 4));
                    canvas.drawRect(left, y - 5 * density, right, y + 5 * density, mPressPaint);
                    canvas.restore();
                    break;
                }
                case 7: { // starlight
                    mPressPaint.setStyle(Paint.Style.FILL);
                    final int seed = Math.abs(key.hashCode());
                    for (int i = 0; i < 6; i++) {
                        float px = left + key.width * (0.15f + 0.7f * ((seed + i * 37) % 100) / 100f);
                        float py = top + key.height * (0.15f + 0.7f * ((seed + i * 61) % 100) / 100f);
                        mPressPaint.setColor(color);
                        mPressPaint.setAlpha(Math.max(0, Color.alpha(color)
                                * (i % 2 == 0 ? 1 : 2) / 3));
                        float radius = (1.2f + 2.2f * (float) Math.abs(Math.sin(now / 160f + i)))
                                * density * fade;
                        canvas.drawCircle(px, py, Math.max(0.6f, radius), mPressPaint);
                    }
                    break;
                }
                case 8: { // shockwave
                    mPressPaint.setStyle(Paint.Style.STROKE);
                    mPressPaint.setStrokeWidth(2f * density);
                    mPressPaint.setColor(color);
                    float grow = 10f * density * t;
                    mPressRect.set(left - grow, top - grow, right + grow, bottom + grow);
                    canvas.drawRoundRect(mPressRect, mKeyCornerRadius + grow,
                            mKeyCornerRadius + grow, mPressPaint);
                    mPressPaint.setAlpha(Math.max(0, Color.alpha(color) / 3));
                    mPressRect.inset(-5 * density * t, -5 * density * t);
                    canvas.drawRoundRect(mPressRect, mKeyCornerRadius + grow,
                            mKeyCornerRadius + grow, mPressPaint);
                    break;
                }
                default:
                    break;
            }
            mPressPaint.setShadowLayer(0, 0, 0, 0);
        }
        if (active && !mPreviewMode) postInvalidateOnAnimation();
    }

    private boolean isEffectEnabled() {
        return LatinIME.sKeyboardSettings != null
                && LatinIME.sKeyboardSettings.keyEffect != EFFECT_NONE;
    }

    /** Draws the selected animated background and schedules the next frame. */
    private void drawBackgroundEffect(Canvas canvas) {
        switch (LatinIME.sKeyboardSettings.keyEffect) {
            case EFFECT_MATRIX:
                drawMatrixEffect(canvas);
                break;
            case EFFECT_STARS:
                drawStarfield(canvas);
                break;
            case EFFECT_AURORA:
                drawAurora(canvas);
                break;
            case EFFECT_SCANLINES:
                drawScanlines(canvas);
                break;
            case EFFECT_RAIN:
                drawRainEffect(canvas);
                break;
            case EFFECT_BUBBLES:
                drawBubblesEffect(canvas);
                break;
            case EFFECT_GRID:
                drawNeonGrid(canvas);
                break;
            case EFFECT_FIREFLIES:
                drawFireflies(canvas);
                break;
            case EFFECT_PLASMA:
                drawPlasma(canvas);
                break;
            case EFFECT_SNOW:
                drawSnow(canvas);
                break;
            case EFFECT_VORTEX:
                drawVortex(canvas);
                break;
            case EFFECT_RIPPLES:
                drawRipples(canvas);
                break;
            case EFFECT_BEAMS:
                drawLightBeams(canvas);
                break;
            case EFFECT_FLOW:
                drawLiquidFlow(canvas);
                break;
            case EFFECT_CONSTELLATION:
                drawConstellation(canvas);
                break;
            default:
                break;
        }
        if (!mPreviewMode) postInvalidateOnAnimation();
    }

    private float[] mParticleX;
    private float[] mParticleY;

    /** Warm drifting motes, each on its own slow orbit. */
    private void drawFireflies(Canvas canvas) {
        fillBase(canvas, 0xFF070A05);
        final long t = System.currentTimeMillis();
        final int count = 26;
        ensureParticles(count);
        final float w = getWidth();
        final float h = getHeight();
        mEffectPaint.setStyle(Paint.Style.FILL);
        mEffectPaint.setShader(null);
        for (int i = 0; i < count; i++) {
            float phase = (float) ((t / 4200.0) + i * 0.37);
            float x = (float) ((0.5 + 0.42 * Math.sin(phase * 1.3 + i)) * w);
            float y = (float) ((0.5 + 0.45 * Math.cos(phase + i * 0.7)) * h);
            float pulse = (float) (0.55 + 0.45 * Math.sin(t / 700.0 + i));
            int alpha = (int) (90 + 150 * pulse);
            mEffectPaint.setColor((alpha << 24) | 0x00FFD98A);
            canvas.drawCircle(x, y, 1.6f + 2.2f * pulse, mEffectPaint);
        }
    }

    /** Slow plasma bands drifting across the panel. */
    private void drawPlasma(Canvas canvas) {
        fillBase(canvas, 0xFF08030F);
        final long t = System.currentTimeMillis();
        final float w = getWidth();
        final float h = getHeight();
        if (w <= 0 || h <= 0) return;
        mEffectPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 4; i++) {
            float shift = (float) Math.sin(t / 2600.0 + i * 1.4);
            float y = (0.15f + i * 0.2f) * h + shift * h * 0.05f;
            int c1 = Color.argb(115, 120, 40, 210);
            int c0 = Color.argb(0, 40, 10, 90);
            mEffectPaint.setShader(new LinearGradient(0, y - h * 0.18f, 0, y + h * 0.18f,
                    new int[]{c0, c1, c0}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, y - h * 0.18f, w, y + h * 0.18f, mEffectPaint);
        }
        mEffectPaint.setShader(null);
    }

    /** Falling snow with a gentle sideways drift. */
    private void drawSnow(Canvas canvas) {
        fillBase(canvas, 0xFF0A1018);
        final long t = System.currentTimeMillis();
        final int count = 90;
        ensureParticles(count);
        final float w = getWidth();
        final float h = getHeight();
        mEffectPaint.setShader(null);
        mEffectPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < count; i++) {
            float speed = 26f + (i % 7) * 18f;
            float y = (t / 1000f * speed + i * 53f) % Math.max(1f, h);
            float x = (i * 71f + (float) Math.sin(t / 1700.0 + i) * 34f) % Math.max(1f, w);
            mEffectPaint.setColor((0xA0 + (i % 6) * 16) << 24 | 0x00F4FAFF);
            canvas.drawCircle(x, y, 1.6f + (i % 4) * 0.9f, mEffectPaint);
        }
    }

    /** Rotating spiral arms around the panel centre. */
    private void drawVortex(Canvas canvas) {
        fillBase(canvas, 0xFF05010A);
        final long t = System.currentTimeMillis();
        final float cx = getWidth() / 2f;
        final float cy = getHeight() / 2f;
        if (cx <= 0 || cy <= 0) return;
        final float maxR = (float) Math.hypot(cx, cy);
        mEffectPaint.setShader(null);
        mEffectPaint.setStyle(Paint.Style.STROKE);
        for (int arm = 0; arm < 3; arm++) {
            Path path = new Path();
            for (int step = 0; step <= 90; step++) {
                float r = maxR * step / 90f;
                double angle = t / 2400.0 + arm * 2.094 + step * 0.09;
                float x = cx + (float) (Math.cos(angle) * r * 0.9);
                float y = cy + (float) (Math.sin(angle) * r * 0.9);
                if (step == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            mEffectPaint.setStrokeWidth(2.2f);
            mEffectPaint.setColor(arm == 0 ? 0x7745C8FF : (arm == 1 ? 0x666C5CFF : 0x5540E0C0));
            canvas.drawPath(path, mEffectPaint);
        }
    }

    /** Expanding rings rising from the bottom edge, like water disturbed in the dark. */
    private void drawRipples(Canvas canvas) {
        fillBase(canvas, 0xFF04141A);
        final long t = System.currentTimeMillis();
        final float w = getWidth();
        final float h = getHeight();
        if (w <= 0 || h <= 0) return;
        final float cx = w / 2f;
        final float cy = h * 0.92f;
        final float maxR = Math.max(w, h) * 0.9f;
        mEffectPaint.setShader(null);
        mEffectPaint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 7; i++) {
            float phase = (float) ((t / 1600.0 + i * 0.42) % 1.0);
            float radius = 24f + phase * maxR;
            int alpha = (int) (150 * (1.0 - phase));
            mEffectPaint.setStrokeWidth(1.5f + 3.5f * (1f - phase));
            mEffectPaint.setColor((alpha << 24) | 0x0056E0D6);
            canvas.drawOval(cx - radius, cy - radius * 0.62f,
                    cx + radius, cy + radius * 0.62f, mEffectPaint);
            canvas.drawOval(cx - radius * 0.55f, cy - radius * 0.34f,
                    cx + radius * 0.55f, cy + radius * 0.34f, mEffectPaint);
        }
    }

    /** Slow searchlight beams sweeping across the panel from below. */
    private void drawLightBeams(Canvas canvas) {
        fillBase(canvas, 0xFF080611);
        final long t = System.currentTimeMillis();
        final float w = getWidth();
        final float h = getHeight();
        if (w <= 0 || h <= 0) return;
        final float pivotX = w * 0.5f;
        final float pivotY = h * 1.18f;
        mEffectPaint.setShader(null);
        mEffectPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 5; i++) {
            double angle = t / 2600.0 + i * 0.62;
            float spread = 0.11f;
            float reach = h * 1.9f;
            float x1 = pivotX + (float) Math.cos(angle - spread) * reach;
            float y1 = pivotY + (float) Math.sin(angle - spread) * reach;
            float x2 = pivotX + (float) Math.cos(angle + spread) * reach;
            float y2 = pivotY + (float) Math.sin(angle + spread) * reach;
            int tint = (i % 2 == 0) ? 0x00FFD9A0 : 0x009BD8FF;
            Path beam = new Path();
            beam.moveTo(pivotX, pivotY);
            beam.lineTo(x1, y1);
            beam.lineTo(x2, y2);
            beam.close();
            mEffectPaint.setShader(new LinearGradient(pivotX, pivotY,
                    (x1 + x2) / 2f, (y1 + y2) / 2f,
                    new int[]{0x44FFFFFF, 0x22FFFFFF, 0x00FFFFFF}, null,
                    Shader.TileMode.CLAMP));
            mEffectPaint.setColor(tint);
            canvas.drawPath(beam, mEffectPaint);
        }
        mEffectPaint.setShader(null);
    }

    /** Silky ribbons of colour drifting sideways over a deep base. */
    private void drawLiquidFlow(Canvas canvas) {
        fillBase(canvas, 0xFF040A0E);
        final long t = System.currentTimeMillis();
        final float w = getWidth();
        final float h = getHeight();
        if (w <= 0 || h <= 0) return;
        mEffectPaint.setStyle(Paint.Style.STROKE);
        mEffectPaint.setStrokeWidth(Math.max(2f, h / 42f));
        final int[] colors = {0x6684F2C8, 0x669BB7FF, 0x66E0A6FF, 0x66FFC98A};
        for (int band = 0; band < 7; band++) {
            Path path = new Path();
            float baseY = h * (0.18f + band * 0.11f);
            for (int step = 0; step <= 24; step++) {
                float x = w * step / 24f;
                double y = baseY
                        + Math.sin(step * 0.42 + t / 1900.0 + band) * h * 0.045
                        + Math.sin(step * 0.17 - t / 2600.0 + band * 0.7) * h * 0.03;
                if (step == 0) path.moveTo(x, (float) y); else path.lineTo(x, (float) y);
            }
            mEffectPaint.setColor(colors[band % colors.length]);
            canvas.drawPath(path, mEffectPaint);
        }
    }

    /** Faint stars joined into a slowly drifting constellation. */
    private void drawConstellation(Canvas canvas) {
        fillBase(canvas, 0xFF05070E);
        final long t = System.currentTimeMillis();
        final float w = getWidth();
        final float h = getHeight();
        if (w <= 0 || h <= 0) return;
        final int count = 32;
        float[] xs = new float[count];
        float[] ys = new float[count];
        for (int i = 0; i < count; i++) {
            float seedX = ((i * 73) % 97) / 97f;
            float seedY = ((i * 41 + 17) % 89) / 89f;
            xs[i] = (seedX * 0.92f + 0.04f) * w + (float) Math.sin(t / 5200.0 + i) * 6f;
            ys[i] = (seedY * 0.86f + 0.07f) * h + (float) Math.cos(t / 4700.0 + i) * 5f;
        }
        mEffectPaint.setStyle(Paint.Style.STROKE);
        mEffectPaint.setStrokeWidth(1f);
        mEffectPaint.setColor(0x2E9FC4FF);
        final float link = Math.min(w, h) * 0.24f;
        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                if (Math.hypot(xs[i] - xs[j], ys[i] - ys[j]) < link) {
                    canvas.drawLine(xs[i], ys[i], xs[j], ys[j], mEffectPaint);
                }
            }
        }
        mEffectPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < count; i++) {
            float twinkle = (float) (0.5 + 0.5 * Math.sin(t / 900.0 + i * 1.7));
            mEffectPaint.setColor(((int) (110 + 140 * twinkle) << 24) | 0x00E8F2FF);
            canvas.drawCircle(xs[i], ys[i], 1.4f + 1.8f * twinkle, mEffectPaint);
        }
    }

    private void ensureParticles(int count) {
        if (mParticleX == null || mParticleX.length < count) {
            mParticleX = new float[count];
            mParticleY = new float[count];
        }
    }

    private static final int THEME_WOOD = 12;

    private boolean isWoodTheme() {
        return LatinIME.sKeyboardSettings != null
                && LatinIME.sKeyboardSettings.keyboardTheme == THEME_WOOD;
    }

    /** Procedural wood fibre: wavy grain lines over the panel and each cap. */
    private void drawWoodGrain(Canvas canvas, float left, float top, float right, float bottom,
                               int alpha) {
        final float width = right - left;
        final float height = bottom - top;
        if (width <= 0 || height <= 0) return;

        mEffectPaint.setShader(null);
        mEffectPaint.setStyle(Paint.Style.STROKE);
        final int lines = Math.max(8, (int) (height / 9));
        for (int i = 0; i < lines; i++) {
            final float baseY = top + (i + 0.5f) * height / lines;
            final float amp = 1.2f + (i % 5) * 1.3f;
            final float phase = i * 0.7f;
            Path path = new Path();
            path.moveTo(left, baseY);
            for (float x = 0; x <= width; x += 8) {
                float y = baseY + (float) Math.sin(x / 34f + phase) * amp
                        + (float) Math.sin(x / 9f + phase * 2f) * (amp * 0.35f);
                path.lineTo(left + x, y);
            }
            final boolean dark = (i % 3) != 0;
            mEffectPaint.setColor(dark
                    ? Color.argb(alpha, 52, 28, 12)
                    : Color.argb(Math.max(1, alpha / 2), 226, 178, 118));
            mEffectPaint.setStrokeWidth(dark ? 1.7f : 1.0f);
            canvas.drawPath(path, mEffectPaint);
        }
    }

    /** Slow drifting stars on a near-black sky. */
    private void drawStarfield(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) return;

        final int starCount = Math.max(60, width * height / 9000);
        if (mStarX == null || mStarX.length != starCount) {
            mStarX = new float[starCount];
            mStarY = new float[starCount];
            mStarSpeed = new float[starCount];
            Random random = new Random();
            for (int i = 0; i < starCount; i++) {
                mStarX[i] = random.nextFloat() * width;
                mStarY[i] = random.nextFloat() * height;
                mStarSpeed[i] = (6 + random.nextInt(22)) / 1000f;
            }
        }

        final long now = System.currentTimeMillis();
        final float delta = mEffectClock == 0 ? 16 : Math.min(64, now - mEffectClock);
        mEffectClock = now;

        fillBase(canvas, 0xFF03060C);
        mEffectPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < mStarX.length; i++) {
            mStarY[i] += mStarSpeed[i] * delta;
            if (mStarY[i] > height + 4) {
                mStarY[i] = -4;
                mStarX[i] = (float) Math.random() * width;
            }
            int alpha = 110 + (i % 6) * 24;
            mEffectPaint.setColor((alpha << 24) | 0x00E6F2FF);
            canvas.drawCircle(mStarX[i], mStarY[i], 1.2f + (i % 4) * 0.7f, mEffectPaint);
        }
    }

    /** Three drifting colour bands, like a slow aurora. */
    private void drawAurora(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) return;

        fillBase(canvas, 0xFF05060A);
        final long time = System.currentTimeMillis();
        mEffectPaint.setStyle(Paint.Style.FILL);

        for (int band = 0; band < 3; band++) {
            final float phase = (time % 14000) / 14000f * 6.2832f + band * 2.1f;
            final float top = height * (0.18f + 0.16f * (float) Math.sin(phase));
            final float bottom = top + height * (0.45f + 0.1f * (float) Math.cos(phase + 1f));
            final float hue = ((time / 90f) + band * 110) % 360;
            final int solid = Color.HSVToColor(new float[] {hue, 0.72f, 0.92f});
            final int clear = solid & 0x00FFFFFF;
            mEffectPaint.setShader(new LinearGradient(0, top, 0, bottom, solid, clear,
                    Shader.TileMode.CLAMP));
            canvas.drawRect(0, top, width, bottom, mEffectPaint);
            mEffectPaint.setShader(null);
        }
    }

    /** Diagonal rain streaks with bright heads. */
    private void drawRainEffect(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) return;
        fillBase(canvas, 0xFF050B14);

        final float density = getResources().getDisplayMetrics().density;
        final long time = System.currentTimeMillis();
        mEffectPaint.setStyle(Paint.Style.STROKE);
        mEffectPaint.setStrokeWidth(1.6f * density);
        for (int i = 0; i < 90; i++) {
            float x = ((i * 97) % width + (time * 0.05f) % width) % width;
            float y = (((i * 53) % height) + time * 0.30f) % (height + 200) - 100;
            int alpha = 30 + (i % 6) * 20;
            mEffectPaint.setColor((alpha << 24) | 0x7FB8FF);
            canvas.drawLine(x, y, x - 9 * density, y + 44 * density, mEffectPaint);
        }
    }

    /** Soft bubbles drifting upward. */
    private void drawBubblesEffect(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) return;
        fillBase(canvas, 0xFF071018);

        final float density = getResources().getDisplayMetrics().density;
        final long time = System.currentTimeMillis();
        mEffectPaint.setStyle(Paint.Style.STROKE);
        mEffectPaint.setStrokeWidth(1.4f * density);
        for (int i = 0; i < 26; i++) {
            float x = (i * 71) % width;
            float drift = (float) Math.sin((time / 1400f) + i) * 12 * density;
            float y = height - (((i * 61) + time * 0.05f) % (height + 200));
            float radius = (5 + (i % 5) * 4) * density;
            int alpha = 24 + (i % 4) * 14;
            mEffectPaint.setColor((alpha << 24) | 0x9FE8FF);
            canvas.drawCircle(x + drift, y, radius, mEffectPaint);
        }
    }

    /** Synthwave perspective grid scrolling toward the viewer. */
    private void drawNeonGrid(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) return;
        fillBase(canvas, 0xFF0B0616);

        final float density = getResources().getDisplayMetrics().density;
        final long time = System.currentTimeMillis();
        final float horizon = height * 0.32f;

        mEffectPaint.setStyle(Paint.Style.STROKE);
        mEffectPaint.setStrokeWidth(1.2f * density);
        mEffectPaint.setColor(0x66FF3CB4);

        for (int i = 1; i <= 12; i++) {
            float t = i / 12f;
            float y = horizon + (height - horizon) * t * t;
            canvas.drawLine(0, y, width, y, mEffectPaint);
        }
        float offset = (time * 0.05f) % (width / 10f);
        for (int i = -10; i <= 10; i++) {
            float x = width / 2f + i * (width / 10f) + offset;
            canvas.drawLine(width / 2f, horizon, x, height, mEffectPaint);
        }
    }

    /** CRT look: dim scanlines with a bright band sweeping down. */
    private void drawScanlines(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) return;

        fillBase(canvas, 0xFF02060A);
        final long time = System.currentTimeMillis();
        final float sweep = (time % 2400) / 2400f * (height + 240) - 120;

        mEffectPaint.setStyle(Paint.Style.FILL);
        mEffectPaint.setShader(new LinearGradient(0, sweep - 110, 0, sweep + 110,
                0x0000FF9A, 0x4400FF9A, Shader.TileMode.CLAMP));
        canvas.drawRect(0, sweep - 110, width, sweep + 110, mEffectPaint);
        mEffectPaint.setShader(null);

        final float step = 4 * getResources().getDisplayMetrics().density;
        mEffectPaint.setColor(0x59000000);
        for (float y = 0; y < height; y += step) {
            canvas.drawRect(0, y, width, y + 1.5f * getResources().getDisplayMetrics().density,
                    mEffectPaint);
        }
    }

    /**
     * Draws the Matrix-style rain behind the keys and schedules the next frame.
     * The head of each column is brightest and the trail fades out above it.
     */
    private void drawMatrixEffect(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) return;

        final float density = getResources().getDisplayMetrics().density;
        final float cell = 20 * density;
        final int columns = Math.max(1, (int) (width / cell));
        if (mMatrixHead == null || mMatrixColumns != columns) {
            mMatrixHead = new float[columns];
            mMatrixSpeed = new float[columns];
            mMatrixGlyph = new char[columns];
            mMatrixColumns = columns;
            Random random = new Random();
            for (int i = 0; i < columns; i++) {
                mMatrixHead[i] = -random.nextInt(Math.max(1, height));
                mMatrixSpeed[i] = (140 + random.nextInt(260)) / 1000f;
                mMatrixGlyph[i] = MATRIX_ALPHABET[random.nextInt(MATRIX_ALPHABET.length)];
            }
        }

        final long now = System.currentTimeMillis();
        float delta = mMatrixLastFrame == 0 ? 16 : Math.min(64, now - mMatrixLastFrame);
        mMatrixLastFrame = now;

        fillBase(canvas, 0xFF000000);
        mMatrixPaint.setTextSize(cell * 0.78f);

        for (int i = 0; i < mMatrixColumns; i++) {
            mMatrixHead[i] += mMatrixSpeed[i] * delta;
            if (mMatrixHead[i] - cell * 12 > height) {
                mMatrixHead[i] = -cell * (2 + (float) Math.random() * 12);
                mMatrixSpeed[i] = (140 + (float) Math.random() * 260) / 1000f;
            }
            if (Math.random() < 0.03) {
                mMatrixGlyph[i] = MATRIX_ALPHABET[(int) (Math.random() * MATRIX_ALPHABET.length)];
            }

            final float x = i * cell + cell / 2f;
            for (int trail = 0; trail < 12; trail++) {
                final float y = mMatrixHead[i] - trail * cell;
                if (y < -cell || y > height + cell) continue;
                if (trail == 0) {
                    mMatrixPaint.setColor(0xFFD8FFE4);
                } else {
                    int alpha = Math.max(0, 210 - trail * 17);
                    mMatrixPaint.setColor((alpha << 24) | 0x0033FF66);
                }
                char glyph = trail == 0
                        ? mMatrixGlyph[i]
                        : MATRIX_ALPHABET[(i * 31 + trail * 17) % MATRIX_ALPHABET.length];
                canvas.drawText(String.valueOf(glyph), x, y, mMatrixPaint);
            }
        }

    }

    /** Label colour from the "Key letter colour" setting, or the theme colour. */
    private int getKeyTextColor() {
        int pref = LatinIME.sKeyboardSettings == null ? 0 : LatinIME.sKeyboardSettings.keyTextColor;
        if (pref > 0 && pref < KEY_TEXT_COLORS.length) {
            return KEY_TEXT_COLORS[pref];
        }
        return mKeyTextColor;
    }

    /** Optional macOS-style symbols for the modifier keys. */
    /** Windows-style modifiers: ⊞ for the Windows key, plain Alt, Esc, Caps. */
    private String windowsGlyph(int style, int code, String text, String label) {
        final boolean spelled = style == 7;
        if (code == LatinKeyboardView.KEYCODE_CTRL_LEFT || "ctrl".equalsIgnoreCase(text)
                || "control".equalsIgnoreCase(text)) {
            return spelled ? "Control" : "Ctrl";
        }
        if (code == LatinKeyboardView.KEYCODE_ALT_LEFT || "alt".equalsIgnoreCase(text)) {
            return "Alt";
        }
        if (code == LatinKeyboardView.KEYCODE_META_LEFT || "meta".equalsIgnoreCase(text)
                || "win".equalsIgnoreCase(text)) {
            // The four-pane Windows mark, not Apple's command symbol.
            return spelled ? "Windows" : "\u229E";
        }
        if (code == LatinKeyboardView.KEYCODE_FN || "fn".equalsIgnoreCase(text)) {
            return spelled ? "Function" : "Fn";
        }
        if (code == 27 || "esc".equalsIgnoreCase(text)) {
            return spelled ? "Escape" : "Esc";
        }
        if (code == 9 || "tab".equalsIgnoreCase(text)) return "Tab";
        if (code == 10 || code == 13 || "enter".equalsIgnoreCase(text)) return "Enter";
        if (code == Keyboard.KEYCODE_SHIFT || "caps".equalsIgnoreCase(text)) {
            return spelled ? "Caps Lock" : "Caps";
        }
        switch (code) {
            case 19: return "\u2191";
            case 20: return "\u2193";
            case 21: return "\u2190";
            case 22: return "\u2192";
            case 23: return "\u00b7";
            default: break;
        }
        return label;
    }

    private String applyGlyphStyle(String label, int[] codes) {
        if (LatinIME.sKeyboardSettings == null || LatinIME.sKeyboardSettings.keyGlyphs == 0) {
            return label;
        }
        final int style = LatinIME.sKeyboardSettings.keyGlyphs;
        final int code = (codes != null && codes.length > 0) ? codes[0] : 0;
        final String text = label == null ? "" : label;

        // Modifier keys are identified by code, not by their label text.
        if (code == LatinKeyboardView.KEYCODE_CTRL_LEFT || "ctrl".equalsIgnoreCase(text)
                || "control".equalsIgnoreCase(text)) {
            switch (style) {
                case 1: return "\u2303";          // ⌃
                case 2: return "Ctl";
                case 3: return "Control";
                case 4: return "\u2388";          // ⎈
                case 5: return "ctrl";
                default: break;
            }
        }
        if (code == LatinKeyboardView.KEYCODE_ALT_LEFT || "alt".equalsIgnoreCase(text)
                || "option".equalsIgnoreCase(text)) {
            switch (style) {
                case 1: case 4: return "\u2325";  // ⌥
                case 2: return "Alt";
                case 3: return "Option";
                case 5: return "alt";
                default: break;
            }
        }
        if (code == LatinKeyboardView.KEYCODE_META_LEFT || "meta".equalsIgnoreCase(text)
                || "super".equalsIgnoreCase(text) || "win".equalsIgnoreCase(text)) {
            switch (style) {
                case 1: case 4: return "\u2318";  // ⌘ Command
                case 2: return "Meta";
                case 3: return "Command";
                case 5: return "cmd";
                default: break;
            }
        }
        if (code == LatinKeyboardView.KEYCODE_FN || "fn".equalsIgnoreCase(text)) {
            switch (style) {
                case 1: return "fn";
                case 4: return "\u0192";          // ƒ
                case 3: return "Function";
                case 5: return "fn";
                default: break;
            }
        }
        if (code == 27 || "esc".equalsIgnoreCase(text) || "escape".equalsIgnoreCase(text)) {
            switch (style) {
                case 1: return "esc";
                case 4: return "\u238b";          // ⎋
                case 3: return "Escape";
                case 5: return "esc";
                default: break;
            }
        }
        if (code == 9 || "tab".equalsIgnoreCase(text)) {
            switch (style) {
                case 1: case 4: return "\u21e5";  // ⇥
                case 2: return "Tab";
                case 3: return "Tab";
                case 5: return "tab";
                default: break;
            }
        }
        if (code == 10 || code == 13 || "enter".equalsIgnoreCase(text)
                || "return".equalsIgnoreCase(text)) {
            switch (style) {
                case 1: return "\u23ce";          // ⏎
                case 4: return "\u2386";          // ⎆
                case 3: return "Return";
                case 5: return "enter";
                default: break;
            }
        }
        if (code == Keyboard.KEYCODE_SHIFT || "caps".equalsIgnoreCase(text)) {
            switch (style) {
                case 1: case 4: return "\u21ea";  // ⇪
                case 3: return "Caps Lock";
                case 5: return "caps";
                default: break;
            }
        }
        // Arrow / D-pad keys (Android keycodes 19-23) follow the glyph style too.
        if (code >= 19 && code <= 23) {
            final String up, down, left, right, centre;
            switch (style) {
                case 1:  up = "\u2191"; down = "\u2193"; left = "\u2190"; right = "\u2192"; centre = "\u00b7"; break;
                case 2:  up = "\u25b2"; down = "\u25bc"; left = "\u25c0"; right = "\u25b6"; centre = "\u25cf"; break;
                case 3:  up = "Up";       down = "Down";     left = "Left";     right = "Right";   centre = "Click"; break;
                case 4:  up = "\u2b06"; down = "\u2b07"; left = "\u2b05"; right = "\u27a1"; centre = "\u2b24"; break;
                case 5:  up = "\u2191"; down = "\u2193"; left = "\u2190"; right = "\u2192"; centre = "\u00b7"; break;
                default: up = null; down = null; left = null; right = null; centre = null; break;
            }
            switch (code) {
                case 19: return up != null ? up : label;
                case 20: return down != null ? down : label;
                case 21: return left != null ? left : label;
                case 22: return right != null ? right : label;
                case 23: return centre != null ? centre : label;
                default: break;
            }
        }
        if (style == 3) {
            return label;
        }
        if (style == 5 && text.length() > 0) {
            return text.toLowerCase(java.util.Locale.US);
        }
        return label;
    }

    /** Typeface for key labels, honouring the "Key font" setting. */
    private Typeface getLabelTypeface(boolean wordLabel) {
        int font = LatinIME.sKeyboardSettings == null ? 0 : LatinIME.sKeyboardSettings.keyFont;
        if (font <= 0) {
            return wordLabel ? Typeface.DEFAULT : mKeyTextStyle;
        }
        return LABEL_TYPEFACES[Math.min(font, LABEL_TYPEFACES.length - 1)];
    }

    private boolean openPopupIfRequired(int keyIndex, PointerTracker tracker) {
        // Check if we have a popup layout specified first.
        if (mPopupLayout == 0) {
            return false;
        }

        Key popupKey = tracker.getKey(keyIndex);
        if (popupKey == null)
            return false;
        if (tracker.isInSlidingKeyInput())
            return false;
        boolean result = onLongPress(popupKey);
        if (result) {
            dismissKeyPreview();
            mMiniKeyboardTrackerId = tracker.mPointerId;
            // Mark this tracker "already processed" and remove it from the pointer queue
            tracker.setAlreadyProcessed();
            mPointerQueue.remove(tracker);
        }
        return result;
    }

    private void inflateMiniKeyboardContainer() {
        //Log.i(TAG, "inflateMiniKeyboardContainer(), mPopupLayout=" + mPopupLayout + " from " + this);
        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View container = inflater.inflate(mPopupLayout, null);

        mMiniKeyboard =
                (LatinKeyboardBaseView)container.findViewById(R.id.LatinKeyboardBaseView);
        mMiniKeyboard.setOnKeyboardActionListener(new OnKeyboardActionListener() {
            public void onKey(int primaryCode, int[] keyCodes, int x, int y) {
                mKeyboardActionListener.onKey(primaryCode, keyCodes, x, y);
                dismissPopupKeyboard();
            }

            public void onText(CharSequence text) {
                mKeyboardActionListener.onText(text);
                dismissPopupKeyboard();
            }

            public void onCancel() {
                mKeyboardActionListener.onCancel();
                dismissPopupKeyboard();
            }

            public boolean swipeLeft() {
                return false;
            }
            public boolean swipeRight() {
                return false;
            }
            public boolean swipeUp() {
                return false;
            }
            public boolean swipeDown() {
                return false;
            }
            public void onPress(int primaryCode) {
                mKeyboardActionListener.onPress(primaryCode);
            }
            public void onRelease(int primaryCode) {
                mKeyboardActionListener.onRelease(primaryCode);
            }
        });
        // Override default ProximityKeyDetector.
        mMiniKeyboard.mKeyDetector = new MiniKeyboardKeyDetector(mMiniKeyboardSlideAllowance);
        // Remove gesture detector on mini-keyboard
        mMiniKeyboard.mGestureDetector = null;

        mMiniKeyboard.setPopupParent(this);

        mMiniKeyboardContainer = container;
    }

    private static boolean isOneRowKeys(List<Key> keys) {
        if (keys.size() == 0) return false;
        final int edgeFlags = keys.get(0).edgeFlags;
        // HACK: The first key of mini keyboard which was inflated from xml and has multiple rows,
        // does not have both top and bottom edge flags on at the same time.  On the other hand,
        // the first key of mini keyboard that was created with popupCharacters must have both top
        // and bottom edge flags on.
        // When you want to use one row mini-keyboard from xml file, make sure that the row has
        // both top and bottom edge flags set.
        return (edgeFlags & Keyboard.EDGE_TOP) != 0 && (edgeFlags & Keyboard.EDGE_BOTTOM) != 0;
    }

    private Keyboard getLongPressKeyboard(Key popupKey) {
        final WeakHashMap<Key, Keyboard> cache;
        if (popupKey.isDistinctCaps()) {
            cache = mMiniKeyboardCacheCaps;
        } else if (popupKey.isShifted()) {
            cache = mMiniKeyboardCacheShift;
        } else {
            cache = mMiniKeyboardCacheMain;
        }
        Keyboard kbd = cache.get(popupKey);
        if (kbd == null) {
            kbd = popupKey.getPopupKeyboard(getContext(), getPaddingLeft() + getPaddingRight());
            if (kbd != null) cache.put(popupKey, kbd);
        }
        //Log.i(TAG, "getLongPressKeyboard returns " + kbd + " for " + popupKey);
        return kbd;
    }

    /**
     * Called when a key is long pressed. By default this will open any popup keyboard associated
     * with this key through the attributes popupLayout and popupCharacters.
     * @param popupKey the key that was long pressed
     * @return true if the long press is handled, false otherwise. Subclasses should call the
     * method on the base class if the subclass doesn't wish to handle the call.
     */
    protected boolean onLongPress(Key popupKey) {
        // TODO if popupKey.popupCharacters has only one letter, send it as key without opening
        // mini keyboard.

        if (mPopupLayout == 0) return false; // No popups wanted

        Keyboard kbd = getLongPressKeyboard(popupKey);
        //Log.i(TAG, "onLongPress, kbd=" + kbd);
        if (kbd == null) return false;

        if (mMiniKeyboardContainer == null) {
            inflateMiniKeyboardContainer();
        }
        if (mMiniKeyboard == null) return false;
        mMiniKeyboard.setKeyboard(kbd);
        mMiniKeyboardContainer.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.AT_MOST));

        if (mWindowOffset == null) {
            mWindowOffset = new int[2];
            getLocationInWindow(mWindowOffset);
        }

        // Get width of a key in the mini popup keyboard = "miniKeyWidth".
        // On the other hand, "popupKey.width" is width of the pressed key on the main keyboard.
        // We adjust the position of mini popup keyboard with the edge key in it:
        //  a) When we have the leftmost key in popup keyboard directly above the pressed key
        //     Right edges of both keys should be aligned for consistent default selection
        //  b) When we have the rightmost key in popup keyboard directly above the pressed key
        //     Left edges of both keys should be aligned for consistent default selection

        final List<Key> miniKeys = mMiniKeyboard.getKeyboard().getKeys();
        final int miniKeyWidth = miniKeys.size() > 0 ? miniKeys.get(0).width : 0;

        int popupX = popupKey.x + mWindowOffset[0];
        popupX += getPaddingLeft();
        if (shouldAlignLeftmost(popupKey)) {
            popupX += popupKey.width - miniKeyWidth;  // adjustment for a) described above
            popupX -= mMiniKeyboardContainer.getPaddingLeft();
        } else {
            popupX += miniKeyWidth;  // adjustment for b) described above
            popupX -= mMiniKeyboardContainer.getMeasuredWidth();
            popupX += mMiniKeyboardContainer.getPaddingRight();
        }
        int popupY = popupKey.y + mWindowOffset[1];
        popupY += getPaddingTop();
        popupY -= mMiniKeyboardContainer.getMeasuredHeight();
        popupY += mMiniKeyboardContainer.getPaddingBottom();
        final int x = popupX;
        final int y = mShowPreview && isOneRowKeys(miniKeys) ? mPopupPreviewDisplayedY : popupY;

        int adjustedX = x;
        if (x < 0) {
            adjustedX = 0;
        } else if (x > (getMeasuredWidth() - mMiniKeyboardContainer.getMeasuredWidth())) {
            adjustedX = getMeasuredWidth() - mMiniKeyboardContainer.getMeasuredWidth();
        }
        // Log.i(TAG, "x=" + x + " y=" + y + " adjustedX=" + adjustedX + " getMeasuredWidth()=" + getMeasuredWidth());
        mMiniKeyboardOriginX = adjustedX + mMiniKeyboardContainer.getPaddingLeft() - mWindowOffset[0];
        mMiniKeyboardOriginY = y + mMiniKeyboardContainer.getPaddingTop() - mWindowOffset[1];
        mMiniKeyboard.setPopupOffset(adjustedX, y);
        mMiniKeyboard.setShiftState(getShiftState());
        // Mini keyboard needs no pop-up key preview displayed.
        mMiniKeyboard.setPreviewEnabled(false);
        mMiniKeyboardPopup.setContentView(mMiniKeyboardContainer);
        mMiniKeyboardPopup.setWidth(mMiniKeyboardContainer.getMeasuredWidth());
        mMiniKeyboardPopup.setHeight(mMiniKeyboardContainer.getMeasuredHeight());
        //Log.i(TAG, "About to show popup " + mMiniKeyboardPopup + " from " + this);
        mMiniKeyboardPopup.showAtLocation(this, Gravity.NO_GRAVITY, adjustedX, y);
        mMiniKeyboardVisible = true;

        // Inject down event on the key to mini keyboard.
        long eventTime = SystemClock.uptimeMillis();
        mMiniKeyboardPopupTime = eventTime;
        MotionEvent downEvent = generateMiniKeyboardMotionEvent(MotionEvent.ACTION_DOWN, popupKey.x
                + popupKey.width / 2, popupKey.y + popupKey.height / 2, eventTime);
        mMiniKeyboard.onTouchEvent(downEvent);
        downEvent.recycle();

        invalidateAllKeys();
        return true;
    }

    private boolean shouldDrawIconFully(Key key) {
        return isNumberAtEdgeOfPopupChars(key) || isLatinF1Key(key)
                || LatinKeyboard.hasPuncOrSmileysPopup(key);
    }

    private boolean shouldDrawLabelAndIcon(Key key) {
        // isNumberAtEdgeOfPopupChars(key) ||
        return isNonMicLatinF1Key(key)
                || LatinKeyboard.hasPuncOrSmileysPopup(key);
    }

    private boolean shouldAlignLeftmost(Key key) {
        return !key.popupReversed;
    }

    private boolean isLatinF1Key(Key key) {
        return (mKeyboard instanceof LatinKeyboard) && ((LatinKeyboard)mKeyboard).isF1Key(key);
    }

    private boolean isNonMicLatinF1Key(Key key) {
        return isLatinF1Key(key) && key.label != null;
    }

    private static boolean isNumberAtEdgeOfPopupChars(Key key) {
        return isNumberAtLeftmostPopupChar(key) || isNumberAtRightmostPopupChar(key);
    }

    /* package */ static boolean isNumberAtLeftmostPopupChar(Key key) {
        if (key.popupCharacters != null && key.popupCharacters.length() > 0
                && isAsciiDigit(key.popupCharacters.charAt(0))) {
            return true;
        }
        return false;
    }

    /* package */ static boolean isNumberAtRightmostPopupChar(Key key) {
        if (key.popupCharacters != null && key.popupCharacters.length() > 0
                && isAsciiDigit(key.popupCharacters.charAt(key.popupCharacters.length() - 1))) {
            return true;
        }
        return false;
    }

    private static boolean isAsciiDigit(char c) {
        return (c < 0x80) && Character.isDigit(c);
    }

    private MotionEvent generateMiniKeyboardMotionEvent(int action, int x, int y, long eventTime) {
        return MotionEvent.obtain(mMiniKeyboardPopupTime, eventTime, action,
                    x - mMiniKeyboardOriginX, y - mMiniKeyboardOriginY, 0);
    }

    /*package*/ boolean enableSlideKeyHack() {
        return false;
    }
    
    private PointerTracker getPointerTracker(final int id) {
        final ArrayList<PointerTracker> pointers = mPointerTrackers;
        final Key[] keys = mKeys;
        final OnKeyboardActionListener listener = mKeyboardActionListener;

        // Create pointer trackers until we can get 'id+1'-th tracker, if needed.
        for (int i = pointers.size(); i <= id; i++) {
            final PointerTracker tracker =
                new PointerTracker(i, mHandler, mKeyDetector, this, getResources(), enableSlideKeyHack());
            if (keys != null)
                tracker.setKeyboard(keys, mKeyHysteresisDistance);
            if (listener != null)
                tracker.setOnKeyboardActionListener(listener);
            pointers.add(tracker);
        }

        return pointers.get(id);
    }

    public boolean isInSlidingKeyInput() {
        if (mMiniKeyboardVisible) {
            return mMiniKeyboard.isInSlidingKeyInput();
        } else {
            return mPointerQueue.isInSlidingKeyInput();
        }
    }

    public int getPointerCount() {
        return mOldPointerCount;
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        return onTouchEvent(me, false);
    }

    public boolean onTouchEvent(MotionEvent me, boolean continuing) {
        final int action = me.getActionMasked();
        final int pointerCount = me.getPointerCount();
        final int oldPointerCount = mOldPointerCount;
        mOldPointerCount = pointerCount;
        
        // TODO: cleanup this code into a multi-touch to single-touch event converter class?
        // If the device does not have distinct multi-touch support panel, ignore all multi-touch
        // events except a transition from/to single-touch.
        if (!mHasDistinctMultitouch && pointerCount > 1 && oldPointerCount > 1) {
            return true;
        }

        // Track the last few movements to look for spurious swipes.
        mSwipeTracker.addMovement(me);

        // Gesture detector must be enabled only when mini-keyboard is not on the screen.
        if (!mMiniKeyboardVisible
                && mGestureDetector != null && mGestureDetector.onTouchEvent(me)) {
            dismissKeyPreview();
            mHandler.cancelKeyTimers();
            return true;
        }

        final long eventTime = me.getEventTime();
        final int index = me.getActionIndex();
        final int id = me.getPointerId(index);
        final int x = (int)me.getX(index);
        final int y = (int)me.getY(index);

        // Needs to be called after the gesture detector gets a turn, as it may have
        // displayed the mini keyboard
        if (mMiniKeyboardVisible) {
            final int miniKeyboardPointerIndex = me.findPointerIndex(mMiniKeyboardTrackerId);
            if (miniKeyboardPointerIndex >= 0 && miniKeyboardPointerIndex < pointerCount) {
                final int miniKeyboardX = (int)me.getX(miniKeyboardPointerIndex);
                final int miniKeyboardY = (int)me.getY(miniKeyboardPointerIndex);
                MotionEvent translated = generateMiniKeyboardMotionEvent(action,
                        miniKeyboardX, miniKeyboardY, eventTime);
                mMiniKeyboard.onTouchEvent(translated);
                translated.recycle();
            }
            return true;
        }

        if (mHandler.isInKeyRepeat()) {
            // It will keep being in the key repeating mode while the key is being pressed.
            if (action == MotionEvent.ACTION_MOVE) {
                return true;
            }
            final PointerTracker tracker = getPointerTracker(id);
            // Key repeating timer will be canceled if 2 or more keys are in action, and current
            // event (UP or DOWN) is non-modifier key.
            if (pointerCount > 1 && !tracker.isModifier()) {
                mHandler.cancelKeyRepeatTimer();
            }
            // Up event will pass through.
        }

        // TODO: cleanup this code into a multi-touch to single-touch event converter class?
        // Translate mutli-touch event to single-touch events on the device that has no distinct
        // multi-touch panel.
        if (!mHasDistinctMultitouch) {
            // Use only main (id=0) pointer tracker.
            PointerTracker tracker = getPointerTracker(0);
            if (pointerCount == 1 && oldPointerCount == 2) {
                // Multi-touch to single touch transition.
                // Send a down event for the latest pointer.
                tracker.onDownEvent(x, y, eventTime);
            } else if (pointerCount == 2 && oldPointerCount == 1) {
                // Single-touch to multi-touch transition.
                // Send an up event for the last pointer.
                tracker.onUpEvent(tracker.getLastX(), tracker.getLastY(), eventTime);
            } else if (pointerCount == 1 && oldPointerCount == 1) {
                tracker.onTouchEvent(action, x, y, eventTime);
            } else {
                Log.w(TAG, "Unknown touch panel behavior: pointer count is " + pointerCount
                        + " (old " + oldPointerCount + ")");
            }
            if (continuing)
                tracker.setSlidingKeyInputState(true);
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            if (!mIgnoreMove) {
                for (int i = 0; i < pointerCount; i++) {
                    PointerTracker tracker = getPointerTracker(me.getPointerId(i));
                    tracker.onMoveEvent((int)me.getX(i), (int)me.getY(i), eventTime);
                }
            }
        } else {
            PointerTracker tracker = getPointerTracker(id);
            switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                mIgnoreMove = false;
                onDownEvent(tracker, x, y, eventTime);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mIgnoreMove = false;
                onUpEvent(tracker, x, y, eventTime);
                break;
            case MotionEvent.ACTION_CANCEL:
                onCancelEvent(tracker, x, y, eventTime);
                break;
            }
            if (continuing)
                tracker.setSlidingKeyInputState(true);
        }

        return true;
    }

    private void onDownEvent(PointerTracker tracker, int x, int y, long eventTime) {
        if (tracker.isOnModifierKey(x, y)) {
            // Before processing a down event of modifier key, all pointers already being tracked
            // should be released.
            mPointerQueue.releaseAllPointersExcept(null, eventTime);
        }
        tracker.onDownEvent(x, y, eventTime);
        mPointerQueue.add(tracker);
    }

    private void onUpEvent(PointerTracker tracker, int x, int y, long eventTime) {
        if (tracker.isModifier()) {
            // Before processing an up event of modifier key, all pointers already being tracked
            // should be released.
            mPointerQueue.releaseAllPointersExcept(tracker, eventTime);
        } else {
            int index = mPointerQueue.lastIndexOf(tracker);
            if (index >= 0) {
                mPointerQueue.releaseAllPointersOlderThan(tracker, eventTime);
            } else {
                Log.w(TAG, "onUpEvent: corresponding down event not found for pointer "
                        + tracker.mPointerId);
            }
        }
        tracker.onUpEvent(x, y, eventTime);
        mPointerQueue.remove(tracker);
    }

    private void onCancelEvent(PointerTracker tracker, int x, int y, long eventTime) {
        tracker.onCancelEvent(x, y, eventTime);
        mPointerQueue.remove(tracker);
    }

    protected boolean swipeRight() {
        return mKeyboardActionListener.swipeRight();
    }

    protected boolean swipeLeft() {
        return mKeyboardActionListener.swipeLeft();
    }

    /*package*/ boolean swipeUp() {
        return mKeyboardActionListener.swipeUp();
    }

    protected boolean swipeDown() {
        return mKeyboardActionListener.swipeDown();
    }

    public void closing() {
        Log.i(TAG, "closing " + this);
        if (mPreviewPopup != null) mPreviewPopup.dismiss();
        mHandler.cancelAllMessages();

        dismissPopupKeyboard();
        //mMiniKeyboardContainer = null; // TODO: destroy/recycle the views?
        //mMiniKeyboard = null;

        // TODO(klausw): use a global bitmap repository, keeping two bitmaps permanently -
        // one for main and one for popup.
        //
        // Allow having the backup bitmap be bigger than the canvas needed, only shrinking in rare cases -
        // for example if reducing the size of the main keyboard.
        //mBuffer = null;
        //mCanvas = null;
        mMiniKeyboardCacheMain.clear();
        mMiniKeyboardCacheShift.clear();
        mMiniKeyboardCacheCaps.clear();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //Log.i(TAG, "onDetachedFromWindow() for " + this);
        closing();
    }

    protected boolean popupKeyboardIsShowing() {
        return mMiniKeyboardPopup != null && mMiniKeyboardPopup.isShowing();
    }

    protected void dismissPopupKeyboard() {
        if (mMiniKeyboardPopup != null) {
            //Log.i(TAG, "dismissPopupKeyboard() " + mMiniKeyboardPopup + " showing=" + mMiniKeyboardPopup.isShowing());
            if (mMiniKeyboardPopup.isShowing()) {
                mMiniKeyboardPopup.dismiss();
            }
            mMiniKeyboardVisible = false;
            mPointerQueue.releaseAllPointersExcept(null, 0); // https://github.com/klausw/hackerskeyboard/issues/477
            invalidateAllKeys();
        }
    }

    public boolean handleBack() {
        if (mMiniKeyboardPopup != null && mMiniKeyboardPopup.isShowing()) {
            dismissPopupKeyboard();
            return true;
        }
        return false;
    }
}
