# Mindful
Mental Health Companion App

## Overview
Mindful is an **Android** application that helps users improve their daily well-being by tracking their mood, goals, and journal entries. Version 2.0 adds **Pattern Insights**, which looks at the user's moods, journal entries, and goals to spot patterns and suggest small, practical steps. Mindful is a self-care tool, not a replacement for professional care.

## Features
### Mood Tracker
- Log your daily mood with the interactive calendar.
- Update today's mood if it changes.
- Review your moods from previous days and months.

### Journal
- Write a daily journal entry that saves automatically.
- Mark an entry complete when you're finished.
- Read your past entries at any time.

### Daily Goals
- Complete daily goals and track your progress.
- See your progress on the home screen.

### Pattern Insights (AI)
- Spots mood trends, such as tough weeks, changes from last week and moods that repeat on certain days.
- Suggests self-care steps based on your patterns.
- Optional AI-written summaries from Google Gemini, sent only with your permission.
- Shows crisis resources (988 Suicide & Crisis Lifeline) if your journal contains concerning language.
- All insights are labeled as non-clinical guidance, never a diagnosis.

### Professional Resources
- Browse and search mental health professionals and their contact information.

### Daily Quotes
- Get an inspirational quote each day on the home screen.

## Technology Used
- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Architecture:** MVVM
- **Local Database:** Room (SQLite)
- **AI:** Google Gemini through Firebase AI Logic, plus on-device pattern analysis
- **Security:** Firebase App Check
- **Tools:** Android Studio, Gradle (Kotlin DSL), JUnit

## Installation and Setup
### Requirements
- Android Studio
- Android SDK (minimum Android 8.0 / API 26, targets API 36)
- An Android emulator (Pixel 10 Pro recommended)

### Steps to Run the Application
1. Clone the repository and open it in Android Studio.
2. Sync the project with Gradle.
3. Open **Device Manager** and start the emulator.
4. Run the application.

The app runs without any extra setup. Pattern Insights uses on-device analysis until Firebase is configured.

### Optional: Enable Gemini AI Insights
1. Create a Firebase project and add an Android app with the package name `com.example.myapplication`.
2. In the Firebase console, go to **AI Services → AI Logic** and enable the **Gemini Developer API**.
3. Download `google-services.json` and place it in the `app/` folder. It is git-ignored, so don't commit it.
4. Run the app, find the App Check debug token in Logcat (search `DebugAppCheckProvider`), and add it in the Firebase console under **Security → App Check → Apps → Manage debug tokens**.

## Contributors
- [Jake Moseley](https://github.com/Jake-Moseley) – Mood Tracker, Daily Goals, Pattern Insights (AI), and project documentation
- [Alex DeWolf](https://github.com/Silencieuxx) – Journal and Professional Resources
- [Kallen Schulz](https://github.com/Kallen1234) – Project setup, UI/UX layout, Home Page and Daily Quotes
