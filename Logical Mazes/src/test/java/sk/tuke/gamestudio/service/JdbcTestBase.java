package sk.tuke.gamestudio.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public abstract class JdbcTestBase {

    // tu si pametam ci DB funguje
    private static boolean dbOk = false;

    @BeforeAll
    static void checkDbConnection() {
        // skusim sa pripojit do DB, ak nejde, testy preskocim
        try (Connection con = DriverManager.getConnection(DBSettings.URL, DBSettings.USER, DBSettings.PASS)) {
            dbOk = true;
        } catch (Exception e) {
            dbOk = false;
        }
    }

    @BeforeEach
    void cleanTablesBeforeEachTest() {
        // ak DB nejde, preskocim test
        Assumptions.assumeTrue(dbOk, "DB nebezi alebo zle DBSettings, preskakujem JDBC testy");

        // vycistim tabulky, aby kazdy test startoval rovnako
        // pozor: toto realne vymaze data
        try (Connection con = DriverManager.getConnection(DBSettings.URL, DBSettings.USER, DBSettings.PASS);
             Statement st = con.createStatement()) {

            st.executeUpdate("DELETE FROM rating");
            st.executeUpdate("DELETE FROM comment");
            st.executeUpdate("DELETE FROM score");

        } catch (Exception e) {
            // ked zlyha cistenie, radsej test preskocim
            Assumptions.assumeTrue(false, "Nepodarilo sa vycistit tabulky: " + e.getMessage());
        }
    }
}