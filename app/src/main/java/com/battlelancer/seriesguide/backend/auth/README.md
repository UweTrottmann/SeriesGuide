# SeriesGuide Cloud Authentication

Currently, using [Firebase Authentication](https://firebase.google.com/docs/auth/android/start).

## Process

Supported to sign up using:

- email and password
- Google sign-in

### Email and password

To sign up, need to specify email address, display name (first and last name) and password.
Requires to verify the email address through a verification email.

To reset the password, an email is sent. The new password is entered on the sent website.

Templates and sender name and address for the emails are configured in the Firebase Authentication
settings.

### Google sign-in

There is currently an issue when signing in using Google to an existing email-based account. The
account is silently changed to Google sign-in, preventing sign in with email. When trying to sign
in with email there is an error that the account doesn't exist.

However, it is possible to sign up again using email to overwrite it back to the email auth 
provider. An alternative is to delete the account in Firebase Authentication and re-create it (Cloud
data is preserved).

## Documentation

- [Firebase for Android Auth API](https://firebase.google.com/docs/reference/android/com/google/firebase/auth/package-summary)
