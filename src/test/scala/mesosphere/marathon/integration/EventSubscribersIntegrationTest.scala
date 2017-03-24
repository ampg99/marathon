package mesosphere.marathon
package integration

import mesosphere.AkkaIntegrationFunTest
import mesosphere.marathon.integration.setup.EmbeddedMarathonTest

@IntegrationTest
class EventSubscribersIntegrationTest extends AkkaIntegrationFunTest with EmbeddedMarathonTest {

  before(cleanUp())

  test("adding an event subscriber") {
    When("an event subscriber is added")
    marathon.subscribe("http://localhost:1337").code should be(200)

    Then("a notification should be sent to all the subscribers")
    waitForEventWith("subscribe_event", { event =>
      if (event.info.exists(_ == "callbackUrl" -> "http://localhost:1337"))
        CallbackMatchSuccess
      else
        CallbackMatchFailure(s"info did not contain callbackUrl -> 'http://localhost:1337'")
    })

    And("the subscriber should show up in the list of subscribers")
    marathon.listSubscribers.value.urls should contain("http://localhost:1337")

    // Cleanup
    marathon.unsubscribe("http://localhost:1337")
  }

  test("adding an invalid event subscriber") {
    When("an invalid event subscriber is added")
    marathon.subscribe("invalid%20URL").code should be(422)

    Then("the subscriber should not show up in the list of subscribers")
    marathon.listSubscribers.value.urls should not contain "invalid URL"
  }

  test("removing an event subscriber") {
    When("an event subscriber is removed")
    marathon.subscribe("http://localhost:1337").code should be(200)
    marathon.listSubscribers.value.urls should contain("http://localhost:1337")
    marathon.unsubscribe("http://localhost:1337").code should be(200)

    Then("a notification should be sent to all the subscribers")
    waitForEventWith("subscribe_event", { event =>
      if (event.info.exists(_ == "callbackUrl" -> "http://localhost:1337"))
        CallbackMatchSuccess
      else
        CallbackMatchFailure(s"event info did not contain callbackUrl -> 'http://localhost:1337'")
    })

    And("the subscriber should not show up in the list of subscribers")
    marathon.listSubscribers.value.urls shouldNot contain("http://localhost:1337")
  }
}
