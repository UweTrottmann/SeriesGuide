package com.battlelancer.seriesguide.widgets;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.Utils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A widget that asks for feedback about the app.
 */
public class FeedbackView extends FrameLayout {

    public interface Callback {
        void onRate();

        void onFeedback();

        void onDismiss();
    }

    @IntDef({ Question.ENJOY, Question.RATE, Question.FEEDBACK })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Question {
        int ENJOY = 0;
        int RATE = 1;
        int FEEDBACK = 2;
    }

    private static final String ACTION_QUESTION_ENJOY = "Enjoy";
    private static final String ACTION_QUESTION_RATE = "Rate";
    private static final String ACTION_QUESTION_FEEDBACK = "Feedback";

    private TextView questionTextView;
    private Button negativeButton;
    private Button positiveButton;

    @Nullable private Callback callback;
    @Question private int question;

    public FeedbackView(Context context) {
        this(context, null);
    }

    public FeedbackView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.feedback_view_include, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        questionTextView = findViewById(R.id.textViewFeedback);
        negativeButton = findViewById(R.id.buttonFeedbackNegative);
        positiveButton = findViewById(R.id.buttonFeedbackPositive);

        negativeButton.setOnClickListener(buttonClickListener);
        positiveButton.setOnClickListener(buttonClickListener);

        setQuestion(Question.ENJOY);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.question = question;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        setQuestion(savedState.question);
    }

    private OnClickListener buttonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (question == Question.ENJOY) {
                if (v == positiveButton) {
                    setQuestion(Question.RATE);
                } else if (v == negativeButton) {
                    setQuestion(Question.FEEDBACK);
                }
            } else {
                if (callback == null) {
                    return;
                }
                if (v == positiveButton) {
                    if (question == Question.RATE) {
                        callback.onRate();
                    } else if (question == Question.FEEDBACK) {
                        callback.onFeedback();
                    }
                } else if (v == negativeButton) {
                    callback.onDismiss();
                }
            }
        }
    };

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void setQuestion(int question) {
        this.question = question;
        if (question != Question.ENJOY) {
            if (question == Question.RATE) {
                questionTextView.setText(
                        Utils.isAmazonVersion() ? R.string.feedback_question_rate_amazon
                                : R.string.feedback_question_rate_google);
            } else if (question == Question.FEEDBACK) {
                questionTextView.setText(R.string.feedback_question_feedback);
            }
            negativeButton.setText(R.string.feedback_action_nothanks);
            positiveButton.setText(R.string.feedback_action_ok);
        } else {
            questionTextView.setText(R.string.feedback_question_enjoy);
            negativeButton.setText(R.string.feedback_action_notreally);
            positiveButton.setText(R.string.feedback_action_yes);
        }
    }

    static class SavedState extends BaseSavedState {
        int question;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            question = in.readInt();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(question);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
