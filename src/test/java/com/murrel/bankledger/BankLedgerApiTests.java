package com.murrel.bankledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API tests: each test drives the real HTTP layer with MockMvc
 * (no server socket needed) against the in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BankLedgerApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Creates an account through the API and returns its generated id. */
    private long createAccount(String name, String openingBalance) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountHolderName": "%s", "openingBalance": %s}
                                """.formatted(name, openingBalance)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    @Test
    void createAccount_returns201WithGeneratedAccountNumber() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountHolderName": "Asha Rao", "openingBalance": 1000.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountHolderName").value("Asha Rao"))
                .andExpect(jsonPath("$.accountNumber").isString())
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void createAccount_withBlankName_returns400() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountHolderName": "", "openingBalance": 100.00}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.accountHolderName").exists());
    }

    @Test
    void getAccount_returnsCreatedAccount() throws Exception {
        long id = createAccount("Vikram Mehta", "250.00");

        mockMvc.perform(get("/api/accounts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountHolderName").value("Vikram Mehta"))
                .andExpect(jsonPath("$.balance").value(250.00));
    }

    @Test
    void getAccount_thatDoesNotExist_returns404() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void listAccounts_includesNewlyCreatedAccount() throws Exception {
        long id = createAccount("Listed User", "10.00");

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(id)).exists());
    }

    @Test
    void deposit_increasesBalance() throws Exception {
        long id = createAccount("Depositor", "100.00");

        mockMvc.perform(post("/api/accounts/{id}/deposit", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 40.50}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(140.50));
    }

    @Test
    void withdraw_decreasesBalance() throws Exception {
        long id = createAccount("Withdrawer", "200.00");

        mockMvc.perform(post("/api/accounts/{id}/withdraw", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 75.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(125.00));
    }

    @Test
    void withdraw_moreThanBalance_returns422AndBalanceUnchanged() throws Exception {
        long id = createAccount("Careful Spender", "50.00");

        mockMvc.perform(post("/api/accounts/{id}/withdraw", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 500.00}
                                """))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/accounts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.00));
    }

    @Test
    void transfer_movesMoneyBetweenAccounts() throws Exception {
        long fromId = createAccount("Sender", "300.00");
        long toId = createAccount("Receiver", "100.00");

        mockMvc.perform(post("/api/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromAccountId": %d, "toAccountId": %d, "amount": 120.00}
                                """.formatted(fromId, toId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts/{id}", fromId))
                .andExpect(jsonPath("$.balance").value(180.00));
        mockMvc.perform(get("/api/accounts/{id}", toId))
                .andExpect(jsonPath("$.balance").value(220.00));
    }

    @Test
    void transfer_toSameAccount_returns400() throws Exception {
        long id = createAccount("Self Transfer", "100.00");

        mockMvc.perform(post("/api/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromAccountId": %d, "toAccountId": %d, "amount": 10.00}
                                """.formatted(id, id)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transactionHistory_recordsDepositAndWithdrawal() throws Exception {
        long id = createAccount("History User", "100.00");

        mockMvc.perform(post("/api/accounts/{id}/deposit", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 25.00}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/accounts/{id}/withdraw", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 10.00}
                                """))
                .andExpect(status().isOk());

        // Opening deposit + deposit + withdrawal, newest first
        mockMvc.perform(get("/api/accounts/{id}/transactions", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$[0].resultingBalance").value(115.00));
    }
}
