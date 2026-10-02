package com.digiq.config;

// Runs the startup sweep that closes out unserved tokens from previous days.
import com.digiq.service.QueueService;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

/**
 * Opens the connection pool when the application starts so a bad database
 * configuration fails loudly in the Tomcat log instead of on a user's first click,
 * and tears the JDBC layer down cleanly when it stops.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().setAttribute("appName", "DigiQ");
        sce.getServletContext().setAttribute("appTagline", "Digital Queue Management System");
        try (Connection c = Database.getConnection()) {
            sce.getServletContext().log("DigiQ: database pool ready (" + c.getMetaData().getURL() + ")");

            // Close out anything the branch left unserved. Doing it at startup means a
            // server restarted each morning tidies yesterday's queue before the first
            // customer ever loads a page.
            int expired = new QueueService().sweepStaleTokens();
            if (expired > 0) {
                sce.getServletContext().log("DigiQ: expired " + expired + " unserved token(s) from previous days");
            }

        } catch (SQLException ex) {
            sce.getServletContext().log(
                    "DigiQ: DATABASE UNAVAILABLE - check src/main/resources/db.properties. " + ex.getMessage(), ex);
        }
    }

    /**
     * Shuts the JDBC layer down in the right order.
     *
     * <p>Closing the pool is not enough on its own. Connector/J starts a helper thread
     * and registers its driver with the JVM-wide {@link DriverManager}; both keep a
     * reference to this web application's class loader, so without this the loader
     * cannot be collected and Tomcat reports a memory leak on every redeploy. That
     * matters in development, where the application is redeployed dozens of times in
     * a session and the leaked loaders eventually exhaust metaspace.</p>
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();

        Database.shutdown();
        stopMySqlCleanupThread(ctx);
        deregisterJdbcDrivers(ctx);

        ctx.log("DigiQ: database pool closed and JDBC layer released");
    }

    /**
     * Stops Connector/J's abandoned-connection cleanup thread.
     *
     * <p>Called reflectively so the application still shuts down cleanly if the driver
     * is swapped for one that has no such thread.</p>
     */
    private void stopMySqlCleanupThread(ServletContext ctx) {
        try {
            Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread")
                    .getMethod("checkedShutdown")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError ex) {
            ctx.log("DigiQ: no MySQL cleanup thread to stop (" + ex.getClass().getSimpleName() + ")");
        }
    }

    /** Unregisters only the drivers this web application loaded, never the container's. */
    private void deregisterJdbcDrivers(ServletContext ctx) {
        ClassLoader mine = Thread.currentThread().getContextClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() != mine) {
                continue;
            }
            try {
                DriverManager.deregisterDriver(driver);
            } catch (SQLException ex) {
                ctx.log("DigiQ: could not deregister " + driver.getClass().getName(), ex);
            }
        }
    }
}
