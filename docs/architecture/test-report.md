# Test Report
## Usecase: Adding a new note
   I run the program, and on the main menu press 1 to add a new note. 
   On the title input prompt, I enter "Groceries" and hit ENTER
   On the content input prompt, I enter "Eggs, Lettuce" and hit ENTER
   I hit ENTER again after the save prompt
   To check that my note is saved, I press 2 in the menu
   My notes is what I expected: 1. Groceries: Eggs, Lettuce
   I press ENTER to return to the menu

## Usecase: Editing a note
   I want to add "Cheese" to my groceries list. I hit 5 on the menu
   I enter "Groceriess" in the title prompt, the program correctly tells me that note isn't found and returns me to the menu
   I press 5 and enter "Groceries" correctly, It shows me the current note content and a prompt underneath
   I type "Eggs, Lettuce, Cheese"

## Usecase: Deleting a note
   I finished grocery shopping and want to delete my note.
   I press 4 on the menu and type "Groceries"
   It shows "Groceries: Eggs, Lettuce, Cheese \n Are you sure you want to delete this note?" I press y
   I press ENTER, and then 2 to check that the note was deleted.
   It displays "No notes available." I press ENTER

## Usecase: Searching notes
   I created two new notes: "Boy Names: Chad, Brad" and "Girl Names: Susie, Riley"
   I press 2 to view all notes to confirm they were saved, and they are.
   I return to the main menu and press 3 to search
   I press 1 in the search menu to search by Title
   I search for "Riley" and it says "No notes available."
   Realizing my mistake, I press 3 to go to the search menu, then 2 to search by content
   I input "RILEY" and it returns what I want "Girl Names: Susie, Riley"
   


# Terminal Text

1. Add a new note
2. View all notes
3. Search notes
4. Delete a note
5. Edit a note
0. Exit
   1
   ---- Add a new note -----
   Enter note Title: Groceries
   Enter note Content: Eggs, Lettuce
   Note saved.

Press Enter to continue...

1. Add a new note
2. View all notes
3. Search notes
4. Delete a note
5. Edit a note
0. Exit
   2
   ---- View all notes -----
   Your Notes:
1. Groceries: Eggs, Lettuce

Press Enter to continue...

1. Add a new note
2. View all notes
3. Search notes
4. Delete a note
5. Edit a note
0. Exit
   5
   ---- Edit a note -----
   Enter the title of the note to edit: Groceriess
   Note not found!

Press Enter to continue...

1. Add a new note
2. View all notes
3. Search notes
4. Delete a note
5. Edit a note
0. Exit
   5
   ---- Edit a note -----
   Enter the title of the note to edit: Groceries
   Current content: Eggs, Lettuce
   Enter new content: Eggs, Lettuce, Cheese
   Note updated successfully!

Press Enter to continue...

1. Add a new note
2. View all notes
3. Search notes
4. Delete a note
5. Edit a note
0. Exit
   4
   ---- Delete a note -----
   Enter the title of the note to delete: Groceries
   Current note: Groceries: Eggs, Lettuce, Cheese
   Are you sure you want to delete this note? (y/n): y
   Note deleted successfully!

Press Enter to continue...

1. Add a new note
2. View all notes
3. Search notes
4. Delete a note
5. Edit a note
0. Exit
   2
   ---- View all notes -----
   No notes available.

Press Enter to continue...

1. Add a new note
2. View all notes
3. Search notes
4. Delete a note
5. Edit a note
0. Exit
   1
   ---- Add a new note -----
   Enter note Title: Boy Names
   Enter note Content: Chad, Brad
   Note saved.

Press Enter to continue...

1. Add a new note
2. View all notes
3. Search notes
4. Delete a note
5. Edit a note
0. Exit
   1
   ---- Add a new note -----
   Enter note Title: Girl Names
   Enter note Content: Susie, Riley
   Note saved.

Press Enter to continue...

1. Add a new note
2. View all notes
3. Search notes
4. Delete a note
5. Edit a note
0. Exit
   2
   ---- View all notes -----
   Your Notes:
1. Boy Names: Chad, Brad
2. Girl Names: Susie, Riley

Press Enter to continue...

1. Add a new note
2. View all notes
3. Search notes
4. Delete a note
5. Edit a note
0. Exit
   3
   Search by:
1. Title
2. Content
3. Tag
0. Back to main menu

1
Enter search term: Riley
No notes available.

Press Enter to continue...

1. Add a new note
2. View all notes
3. Search notes
4. Delete a note
5. Edit a note
0. Exit
   3
   Search by:
1. Title
2. Content
3. Tag
0. Back to main menu

2
Enter search term: RILEY
Your Notes:
1. Girl Names: Susie, Riley

Press Enter to continue...

1. Add a new note
2. View all notes
3. Search notes
4. Delete a note
5. Edit a note
0. Exit
