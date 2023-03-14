Feature: Blocks Integration Tests
  Scenario: Evaluate Block Methods

    Given that the latest block is requested
    Then a valid block is returned
    And with a valid hash
    And with a valid body
    And with valid headers
    And with valid proofs

    Given that a block is returned by hash
    Then a valid block is returned
    And with a valid hash
    And with a valid body
    And with valid headers
    And with valid proofs

    Given that a block is returned by height 2
    Then a valid block is returned
    And with a valid hash
    And with a valid body
    And with valid headers
    And with valid proofs

    Given that a block is returned by height 3
    Then a valid block is returned
    And with a valid hash
    And with a valid body
    And with valid headers
    And with valid proofs

    Given that a block is returned by height 4
    Then a valid block is returned
    And with a valid hash
    And with a valid body
    And with valid headers
    And with valid proofs

    Given that an invalid block hash is requested
    Then a valid error message is returned

    Given that an invalid block height is requested
    Then a valid error message is returned


#    Given that a transfer block is requested
#    Then a valid block is returned
#    And with a valid hash
#    And with a valid body
#    And with valid headers
#    And with valid proofs

#    Given I have the block at era switch
#    Then block returns required data


