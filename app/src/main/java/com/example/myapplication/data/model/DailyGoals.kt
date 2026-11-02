package com.example.myapplication.data.model

import java.time.LocalDate
import kotlin.random.Random

/**
 * Pool of small, achievable self-care goals. A few are picked each day,
 * the same way the daily quote rotates.
 */
object DailyGoals {

    private val pool = listOf(
        "Walk 3,000 Steps",
        "Meditate For 10 Minutes",
        "Make Your Bed",
        "Drink 6 Glasses Of Water",
        "Spend 10 Minutes Outside",
        "Text A Friend Or Family Member",
        "Write One Thing You're Grateful For",
        "Stretch For 5 Minutes",
        "No Phone For The First 30 Minutes Awake",
        "Eat A Fruit Or Vegetable",
        "Tidy One Small Space",
        "Take 10 Deep Breaths",
        "Listen To A Song You Love",
        "Go To Bed 30 Minutes Earlier",
        "Read For 15 Minutes",
        "Write A Journal Entry",
        "Log Today's Mood",
        "Compliment Someone",
        "Take A Screen-Free Break",
        "Do Something Creative For 15 Minutes",
        "Step Away From Social Media For An Hour",
        "Eat A Meal Without Screens",
        "Do One Thing You've Been Putting Off",
        "Spend 5 Minutes In Silence",
        "Get Some Sunlight Before Noon",
        "Do A Short Workout",
        "Plan Tomorrow In 3 Bullet Points",
        "Say No To One Thing You Don't Need To Do",
        "Write Down One Thing You Did Well",
        "Try A New Song, Podcast, Or Recipe"
    )

    /**
     * Returns [count] goals for the given day. The choice is seeded by the date,
     * so every screen gets the same goals all day and they change at midnight.
     */
    fun forDate(date: LocalDate = LocalDate.now(), count: Int = 3): List<Goal> {
        val random = Random(date.toEpochDay())
        return pool.shuffled(random)
            .take(count)
            .mapIndexed { index, title -> Goal(id = index + 1, title = title, isCompleted = false) }
    }
}
