# SeriesGuide Cloud Authentication

Currently, using Firebase Authentication to obtain an email address to use for Cloud.

## Documentation

- [Firebase Authentication](https://firebase.google.com/docs/auth/android/start)

- [Firebase for Android Auth API](https://firebase.google.com/docs/reference/android/com/google/firebase/auth/package-summary)

  - [FirebaseAuth](https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuth)

  - [FirebaseAuthException](https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuthException)

## Providers

Supported to sign up using:

- [email and password](https://firebase.google.com/docs/auth/android/password-auth)
- [Google Sign-In](https://firebase.google.com/docs/auth/android/google-signin)

### Email and password

To sign up, need to specify a display name, email address and password.
Currently, doesn't require to verify the email address through a verification email.
Currently, doesn't support multi-factor authentication.

To reset the password, an email is sent with a link to a password reset website.

Templates and sender name and address for the emails as well as password requirements (currently 
default) are configured in the Firebase Authentication settings.

The UI currently requires the password to be at least 15 characters, 
see [FirebaseAuthActivity](FirebaseAuthActivity.kt).

### Google Sign-In

To sign up, need to have a Google account set up in Android.

> [!IMPORTANT]
> Note that currently when signing in using Google to an existing email-based account and the email
address is provided by Gmail, Firebase **converts the account to Google Sign-In**. So signing in
using email (like on another device without Play Services) will then fail unless the password is
reset.
>
> This is because Firebase considers Google the trusted provider for these email addresses. See:
>
> * https://github.com/firebase/FirebaseUI-Android/issues/1180
> * https://groups.google.com/g/firebase-talk/c/ms_NVQem_Cw/m/8g7BFk1IAAAJ
