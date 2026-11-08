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

  Scenario: Successful Twitch callback confirms the connection
    Given the frontend dashboard is open
    When I return after Twitch authorization
    Then the dashboard confirms the Twitch connection
