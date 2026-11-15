package org.pocketworkstation.pckeyboard;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/** Thumbnail picker for the bundled background art. */
public class BackgroundImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_images);
        EdgeInsets.apply(this, false);

        final float density = getResources().getDisplayMetrics().density;
        GridLayout grid = findViewById(R.id.background_grid);
        int selected = BackgroundImages.selected(this);
        final String[] names = getResources().getStringArray(R.array.background_image_entries);

        for (int i = 0; i < BackgroundImages.DRAWABLES.length; i++) {
            final int index = i;
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(4, density), dp(4, density), dp(4, density), dp(4, density));
            params.height = dp(122, density);

            if (i == 0) {
                TextView none = new TextView(this);
                none.setText(R.string.background_none);
                none.setGravity(Gravity.CENTER);
                none.setTextColor(0xFFEDEDED);
                none.setBackgroundResource(R.drawable.bg_none_tile);
                if (selected == 0) none.setAlpha(1f);
                none.setOnClickListener(v -> choose(0));
                grid.addView(none, params);
                continue;
            }

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setOnClickListener(v -> choose(index));

            ImageView tile = new ImageView(this);
            tile.setScaleType(ImageView.ScaleType.CENTER_CROP);
            tile.setImageResource(BackgroundImages.DRAWABLES[i]);
            tile.setBackgroundResource(selected == i
                    ? R.drawable.bg_tile_selected : R.drawable.bg_tile);
            tile.setAlpha(selected == i ? 1f : 0.86f);
            cell.addView(tile, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(88, density)));

            TextView caption = new TextView(this);
            caption.setText(i < names.length ? names[i] : Integer.toString(i));
            caption.setTextSize(11);
            caption.setGravity(Gravity.CENTER);
            caption.setTextColor(0xFFD6D6D6);
            caption.setMaxLines(1);
            cell.addView(caption);

            cell.setContentDescription(getString(R.string.background_image_pick, i));
            grid.addView(cell, params);
        }
    }

    private void choose(int index) {
        BackgroundImages.select(this, index);
        Toast.makeText(this, R.string.background_image_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private int dp(int value, float density) {
        return Math.round(value * density);
    }
}
