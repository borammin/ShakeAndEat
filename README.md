# Shake & Eat

Your lunch group can't decide? Add nearby places, vote, then shake the phone to pick the winner.

Kotlin, Jetpack Compose, Firebase (Authentication and Cloud Firestore).

## Run the app

1. Open this folder in Android Studio and let Gradle sync. Use a Gradle JDK of 17 or newer.
2. Put your Firebase config at `app/google-services.json` (see the next section). The build fails without it.
3. Run the `app` configuration on an emulator or a phone (Android 7.0 or newer).

## Security rules

`firestore.rules` holds the rules, and `firestore-tests/` tests them against the Firebase emulator (needs Node and Java):

| Collection | Read | Write |
|---|---|---|
| `users/{uid}` | any signed-in user | only your own document |
| `groups/{gid}` | members | create as owner; members record the decision; a newcomer may only add their own uid; owner deletes |
| `groups/{gid}/options/{oid}` | members | members add options as themselves; a vote changes only your own uid in `voteUids` |
| `groups/{gid}/messages/{mid}` | members | members create as themselves, up to 500 characters; never edited or deleted |

Everything else is denied. The group ID doubles as the invite code.

## AI usage note