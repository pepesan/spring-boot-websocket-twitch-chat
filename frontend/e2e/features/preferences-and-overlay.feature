Feature: Frontend preferences and OBS overlay
  The interface remains useful without a running backend.

  Scenario: Language preference survives reload
    Given the frontend dashboard is open
    When I choose Spanish
    And I reload the page
    Then the dashboard is shown in Spanish

  Scenario: Theme preference survives reload
    Given the frontend dashboard is open
    When I choose the light theme
    And I reload the page

    Then the light theme is active

  Scenario: Navigate to the read-only OBS overlay
    Given the frontend dashboard is open
    When I open the OBS overlay
    Then the local development overlay is shown

  Scenario: Register or sign in before connecting a platform
    Given the frontend dashboard is open
    Then registration is required before Twitch can be connected


  Scenario: Direct chat access returns anonymous users to account registration
    Given the frontend dashboard is open
    When I open the protected live chat directly
    Then I am returned to account registration
