package com.shkurta.pickle

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.shkurta.pickle.domain.model.*
import com.shkurta.pickle.ui.create.*
import com.shkurta.pickle.ui.home.*
import com.shkurta.pickle.ui.templates.TemplatesContent
import com.shkurta.pickle.ui.theme.PickleTheme
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Instant

/** Offline fixtures only: visual checks never write to the real account or database. */
class HomeTemplatesInstrumentedTest {
    @get:Rule val compose=createComposeRule()
    private val now=Instant.now()
    private fun poll(id:String,question:String,counts:List<Int>,closed:Boolean=false)=Poll(
        id,question,"creator",counts.mapIndexed { i,count -> PollOption("$id-$i",listOf("Detroit-style","Margherita","Pepperoni","Veggie")[i],OptionStatus.APPROVED,count,createdAt=now) },
        PollSettings(allowSuggestions=true),if(closed) PollStatus.CLOSED else PollStatus.OPEN,now,
        closedAt=if(closed) now.minusSeconds(86400) else now.plusSeconds(10800))
    private val open by lazy { listOf(
        poll("pizza","Pizza night: what are we ordering?",listOf(5,3,2,1)).let {it.copy(options=it.options+listOf("one","two").map {id->PollOption(id,"Suggestion",OptionStatus.PENDING,createdAt=now)})},
        poll("film","Film night: what are we watching?",listOf(3,3))) }
    private val closed by lazy {listOf(poll("drinks","Friday drinks: where?",listOf(7,2),true),poll("ski","Ski trip: which weekend?",listOf(5,3),true))}
    private fun template(name:String,id:String,rounds:Int)=PollTemplate(id,"source",name,id,"$name: what are we watching?",listOf("Arrival","Lady Bird","Spirited Away","The Grand Budapest Hotel"),PollSettings(),rounds,WeeklySchedule(5,"20:00","Europe/Athens"))
    private fun render(dark:Boolean=false,narrow:Boolean=false,large:Boolean=false,content:@Composable ()->Unit) {
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,if(large) 1.5f else 1f)) {
                PickleTheme(darkTheme=dark) {Box(Modifier.width(if(narrow) 320.dp else 390.dp).fillMaxHeight().testTag("fixture")) {content()}}
            }
        }
    }
    private fun capture(name:String) {
        compose.waitForIdle()
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val file=File(context.getExternalFilesDir(null),"screenshots/$name.png");file.parentFile!!.mkdirs()
        file.outputStream().use {compose.onNodeWithTag("fixture").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    private fun home(state:HomeUiState) {render {HomeContent(state,{}, {}, {}, {}, {})}}
    @Test fun failedLoadingNeverShowsTheFirstRunGuide() {
        render {HomeContent(HomeUiState(hasLoaded=false),{}, {}, {}, {}, {},loadError="Offline")}
        compose.onNodeWithText("Your polls couldn’t be loaded. Use Retry below.").assertIsDisplayed()
        compose.onNodeWithText("Start your first poll").assertDoesNotExist()
    }
    @Test fun firstRun() {
        home(HomeUiState(hasLoaded=true,displayName="Klaud"))
        compose.onNodeWithText("Start your first poll").assertIsDisplayed();capture("home-first-run")
    }
    @Test fun returningHome() {
        home(HomeUiState(hasLoaded=true,hasAnyPolls=true,displayName="Klaud",polls=open,settledPolls=closed))
        compose.onNodeWithText("2 suggestions to review").assertIsDisplayed();capture("home-my-polls")
    }
    @Test fun onlyClosedHome() {
        home(HomeUiState(hasLoaded=true,hasAnyPolls=true,displayName="Klaud",settledPolls=closed))
        compose.onNodeWithText("No live polls right now.",substring=true).assertIsDisplayed();capture("home-only-closed")
    }
    @Test fun personalTemplates() {
        render {TemplatesContent(listOf(template("Film night","film",3),template("Pizza night","pizza",2)),emptyList(),{}, {_,_->})}
        compose.onNodeWithText("Your templates").assertIsDisplayed();capture("templates-personal")
    }
    @Test fun emptyTemplatesInDarkMode() {
        render(dark=true) {TemplatesContent(emptyList(),emptyList(),{}, {_,_->})}
        compose.onNodeWithText("Save one of your polls",substring=true).assertIsDisplayed();capture("templates-empty-dark")
    }
    @Test fun filmDraft() {
        render {TemplateDraftEditor(CreatePollUiState(question="Film night: what are we watching?",options=listOf("Arrival","Lady Bird","Spirited Away","The Grand Budapest Hotel"),
            title="Film night #4",history="Options carried over from last Friday",winnerText="Paddington 2",excludeWinner=true,
            runnerUpTexts=setOf("Arrival"),newTexts=setOf("The Grand Budapest Hotel"),customClosesAt=now.plusSeconds(10800).toString()),{}, {})}
        compose.onNodeWithText("Create & get the link").assertIsDisplayed();capture("template-film-draft")
    }
    @Test fun narrowLargeTextKeepsPrimaryActionReachable() {
        render(narrow=true,large=true) {HomeContent(HomeUiState(hasLoaded=true,displayName="Klaud"),{}, {}, {}, {}, {})}
        compose.onNodeWithText("Start your first poll").assertIsDisplayed();capture("home-narrow-large-text")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Your polls will live here"))
        compose.onNodeWithText("Your polls will live here").assertIsDisplayed()
    }
    @Test fun homeTemplatesDraftCreationNavigation() {
        render {
            var page by remember {mutableStateOf("home")}
            when(page) {
                "home"->HomeContent(HomeUiState(hasLoaded=true,hasAnyPolls=true,settledPolls=closed),{if(it==HomeFilter.TEMPLATES) page="templates"},{},{},{},{})
                "templates"->TemplatesContent(emptyList(),emptyList(),{page="home"},{_,_->page="draft"})
                "draft"->TemplateDraftEditor(CreatePollUiState(title="Film night",question="Film?",options=listOf("Arrival","Lady Bird")),{page="templates"},{page="created"})
                else->Text("Created fixture poll")
            }
        }
        compose.onNodeWithText("Templates").performClick()
        compose.onNodeWithText("Film night").performClick()
        compose.onNodeWithText("Create & get the link").performClick()
        compose.onNodeWithText("Created fixture poll").assertIsDisplayed()
    }
}
