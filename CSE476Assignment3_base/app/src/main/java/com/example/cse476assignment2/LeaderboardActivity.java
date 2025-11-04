package com.example.cse476assignment2;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

// The following code is written based on YouTube videos I watched
// ChatGPT was used to beautify code and add comments in places they were not in already
public class LeaderboardActivity extends AppCompatActivity {
    // GestureDetector will be used to detect swipe gestures
    private GestureDetector gestureDetector;                    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the leaderboard screen layout
        setContentView(R.layout.activity_leaderboard);

        // Set up the back button (in the UI)
        ImageButton backButton = findViewById(R.id.backButton);
        // When tapped, it simply closes this activity and returns to the previous screen
        backButton.setOnClickListener(v -> finish());

        // Initialize a gesture detector to handle swipe gestures
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            // Constants for swipe detection
            private static final int SWIPE_THRESHOLD = 100;   // minimum swipe distance
            private static final int SWIPE_VELOCITY_THRESHOLD = 100; // minimum speed

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // e1 = starting touch, e2 = ending touch
                if (e1 != null && e2 != null) {
                    float diffX = e2.getX() - e1.getX(); // Horizontal swipe distance
                    float diffY = e2.getY() - e1.getY(); // Vertical swipe distance

                    // Detect swipe RIGHT (horizontal swipe that passes thresholds)
                    if (Math.abs(diffX) > Math.abs(diffY) &&  // Swipe is more horizontal than vertical
                            Math.abs(diffX) > SWIPE_THRESHOLD &&  // Long enough
                            Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD &&  // Fast enough
                            diffX > 0) {                                    // Moved to the right

                        finish(); // 👈 Close this activity (acts like a back gesture)
                        return true;
                    }
                }
                return false; // Didn’t detect a valid swipe
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Pass all touch events to the gesture detector first.
        // If it handles the event, great — otherwise fall back to normal handling.
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }


}