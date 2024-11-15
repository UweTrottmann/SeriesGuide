# Guidelines

Collecting design decisions. New and updated code and resources should follow them.

## Layout resources

View IDs should be unique across the project to support refactoring using Android Studio.

Example: `textViewItemEpisodeTitle` in `item_episode.xml`

Use dimension resources (like `@dimen/default_padding`) for margin and padding to avoid looking them up.

### Icons

Use [Material Icons](https://fonts.google.com/icons) with

- **Rounded** style
- weight 400
- no grade
- typically 24dp size

Some existing icons may still use the old Filled or the old non-rounded Outlined style.

Name icon resource files like `ic_<name>_<tint>_<size>dp.xml`,
for example `ic_event_control_24dp.xml`.

Load vector drawables [using compat loading](https://medium.com/androiddevelopers/using-vector-assets-in-android-apps-4318fd662eb9)
so they work (tinting) and do not crash (gradients) on all supported releases:

- `Button`: use `ViewTools.setVectorDrawableTop`, ...
- `ImageView`, `ImageButton`: use `app:srcCompat`
- `TextView`: use `app:drawableStartCompat`, `app:drawableTopCompat`, ...

## Click listeners

The interface class is owned by the class that owns the views that trigger the click events, for
example the item view holder.

The interface class is named based on what the listener is for.

Example: `ItemClickListener`

The methods are named based on what is clicked.

Example: `onMoreOptionsClick`.

## Dialogs

Using `AppCompatDialogFragment` and overriding `onCreateView` will use `dialogTheme` of the theme.

If possible, use an alert dialog with a custom layout instead for improved sizing, easy adding of title and buttons.

## Alert dialogs (recommended)

Using `AppCompatDialogFragment` and overriding `onCreateDialog` with `MaterialAlertDialogBuilder`.

The dialog theme is `materialAlertDialogTheme`, which only sets `android:windowMinWidthMajor` and `android:windowMinWidthMinor`.

The layout used is `abc_alert_dialog_material.xml` defined via `alertDialogStyle` of the theme.

When using a custom layout via `setView()`:

- `layout_width` and `layout_height` of the root view are overwritten to `match_parent` (see `androidx.appcompat.app.AlertController#setupCustomContent`)
- its parents use `layout_width="match_parent"`, `layout_height="wrap_content"`, `minHeight="48dp"` (see `abc_alert_dialog_material.xml`)
- its `customPanel` parent is resized to take only as much space as is available (see `AlertDialogLayout`)

## PopupMenu

Use `androidx.appcompat.widget.PopupMenu` so Material 3 styles are correctly applied.
(Could also use the platform one and set Widget.Material3.PopupMenu with android:popupMenuStyle,
but rather use the same implementation on all versions.)

## TextInputLayout

When trying to make `TextInputLayout` (grow to) fill available height, if the contained `TextInputEditText` is too tall the counter or error text can get pushed outside of its bounds.

As it is a `LinearLayout`, [until this is fixed](https://github.com/material-components/material-components-android/issues/1435) resolve by using `android:layout_weight="1"` on the contained `TextInputEditText`:

```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/textFieldEditNote"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1"
    app:counterEnabled="true"
    app:counterMaxLength="500">

    <!-- This can grow up to the height of the TextInputLayout by default and will push the
        counter out of bounds. As TextInputLayout is a LinearLayout, set layout_weight="1"
        to resolve. -->
    <com.google.android.material.textfield.TextInputEditText
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_weight="1" />

</com.google.android.material.textfield.TextInputLayout>
```
