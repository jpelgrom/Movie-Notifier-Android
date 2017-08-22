# Movie Notifier Android

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://github.com/jpelgrom/Movie-Notifier-Android/blob/master/LICENSE.md)

An Android front end application for interacting with a [Movie Notifier](https://github.com/SijmenHuizenga/Movie-Notifier) instance.

## Building

 - *Requires* Android Stuido 3.0 Beta 2 or newer.
 - Make sure to add a `gradle.properties` file in the root of the project. An example of the file is [included](https://github.com/jpelgrom/Movie-Notifier-Android/blob/master/gradle.properties.example).
 - The cinema ID filter (referred to as 'location' in the app) currently requires the user to pick a value from an auto-suggest list. Make sure to include this list as `cinemas.json` in the assets folder! Without it, users will not be able to save any watcher because no matching cinema(s) can be found. An example of the file is [included](https://github.com/jpelgrom/Movie-Notifier-Android/blob/master/app/src/debug/assets/cinemas.json.example) in the debug flavor of the app.