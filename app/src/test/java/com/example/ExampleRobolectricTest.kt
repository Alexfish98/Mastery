package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Mastery", appName)
  }

  @Test
  fun `sm2 spaced repetition calculates correct intervals`() {
    val initialCard = com.example.data.local.Flashcard(
        id = 1,
        deckName = "Physics",
        frontText = "What is Newton's First Law?",
        backText = "Inertia",
        interval = 0,
        easeFactor = 2.5,
        dueDate = 0L
    )

    // Review again (score 1) resets interval to 1
    val againUpdated = com.example.data.repository.FlashcardRepository.calculateNextReview(initialCard, 1)
    assertEquals(1, againUpdated.interval)
    org.junit.Assert.assertTrue(againUpdated.easeFactor < 2.5)

    // Review good (score 3) advances interval to 3 for first repetition
    val goodUpdated = com.example.data.repository.FlashcardRepository.calculateNextReview(initialCard, 3)
    assertEquals(3, goodUpdated.interval)

    // Review good second time advances interval to (3 * 2.5).toInt() = 7
    val secondGood = com.example.data.repository.FlashcardRepository.calculateNextReview(goodUpdated, 3)
    assertEquals(7, secondGood.interval)
  }

  @Test
  fun `sandbox storage helper resolves directory and filenames`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sandboxDir = com.example.util.SandboxStorageHelper.getSandboxDir(context)
    org.junit.Assert.assertTrue(sandboxDir.exists())
    
    val testFile = com.example.util.SandboxStorageHelper.saveTextToSandbox(context, "test_notes.txt", "Sample EUEE Notes")
    org.junit.Assert.assertNotNull(testFile)
    org.junit.Assert.assertTrue(testFile!!.exists())
    assertEquals("Sample EUEE Notes", testFile.readText())
    
    val files = com.example.util.SandboxStorageHelper.getSandboxedFiles(context)
    org.junit.Assert.assertTrue(files.any { it.name == "test_notes.txt" })
    
    com.example.util.SandboxStorageHelper.deleteSandboxedFile(testFile)
  }

  @Test
  fun `main activity launches successfully`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()
    org.junit.Assert.assertNotNull(activity)
  }
}
