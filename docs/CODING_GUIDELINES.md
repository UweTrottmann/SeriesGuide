# Coding Guidelines

Collecting design decisions. New and updated code should follow them.

## Layout resources

View IDs should be unique across the project to support refactoring using Android Studio.

Example: `textViewItemEpisodeTitle` in `item_episode.xml`

## Click listeners

The interface class is owned by the class that owns the views that trigger the click events, for
example the item view holder.

The interface class is named based on what the listener is for.

Example: `ItemClickListener`

The methods are named based on what is clicked.

Example: `onMoreOptionsClick`.
