# Coding Guidelines

Collecting design decisions. New and updated code should follow them.

## Click listeners

The interface class is owned by the class that owns the views that trigger the click events, for
example the item view holder.

The interface class is named based on what the listener is for.  
Example: `ItemClickListener`

The methods are named based on what is clicked.  
Example: `onMoreOptionsClick`.
