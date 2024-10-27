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

## Click listeners

The interface class is owned by the class that owns the views that trigger the click events, for
example the item view holder.

The interface class is named based on what the listener is for.

Example: `ItemClickListener`

The methods are named based on what is clicked.

Example: `onMoreOptionsClick`.

## PopupMenu

Use `androidx.appcompat.widget.PopupMenu` so Material 3 styles are correctly applied.
(Could also use the platform one and set Widget.Material3.PopupMenu with android:popupMenuStyle,
but rather use the same implementation on all versions.)
