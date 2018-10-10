package org.vaadin.leif.themecookie;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.Cookie;

import com.vaadin.server.ServiceInitEvent;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.server.UIProviderEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServiceInitListener;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WidgetsetInfo;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.UI;

/**
 * Changes the theme and stores the selection in a cookie.
 */
public class ThemeCookie {
    private static final String THEME_COOKIE = "themeCookie";

    private static final String ACTUAL_PROVIDER_ATTRIBUTE = ProxyUiProvider.class
            .getName() + ".actual";

    /**
     * UI provider that delegates all choices except the theme to a regular UI
     * provider.
     */
    private static final class ProxyUiProvider extends UIProvider {
        private final List<UIProvider> actualProviders;

        public ProxyUiProvider(VaadinSession session) {
            actualProviders = session.getUIProviders();
        }

        @Override
        public Class<? extends UI> getUIClass(UIClassSelectionEvent event) {
            for (UIProvider actualProvider : actualProviders) {
                if (actualProvider instanceof ProxyUiProvider) {
                    continue;
                }

                Class<? extends UI> uiClass = actualProvider.getUIClass(event);
                if (uiClass != null) {
                    /*
                     * Remember which UI provider was used for all upcoming
                     * queries during the same request.
                     */
                    event.getRequest().setAttribute(ACTUAL_PROVIDER_ATTRIBUTE,
                            actualProvider);
                    return uiClass;
                }
            }
            return null;
        }

        private static UIProvider actualProvider(UIProviderEvent event) {
            UIProvider provider = (UIProvider) event.getRequest()
                    .getAttribute(ACTUAL_PROVIDER_ATTRIBUTE);
            if (provider == null) {
                throw new IllegalStateException(
                        "getUIClass should be the first called UI provider method");
            }
            return provider;
        }

        @Override
        public String getTheme(UICreateEvent event) {
            // Use cookie value, or else fall back to actual UI provider
            return findThemeCookieValue(event.getRequest())
                    .orElseGet(() -> actualProvider(event).getTheme(event));
        }

        @Override
        public UI createInstance(UICreateEvent event) {
            return actualProvider(event).createInstance(event);
        }

        @Override
        public String getPageTitle(UICreateEvent event) {
            return actualProvider(event).getPageTitle(event);
        }

        @Override
        public PushMode getPushMode(UICreateEvent event) {
            return actualProvider(event).getPushMode(event);
        }

        @Override
        public Transport getPushTransport(UICreateEvent event) {
            return actualProvider(event).getPushTransport(event);
        }

        @Override
        public WidgetsetInfo getWidgetsetInfo(UICreateEvent event) {
            return actualProvider(event).getWidgetsetInfo(event);
        }

        @Override
        @SuppressWarnings("deprecation")
        public String getWidgetset(UICreateEvent event) {
            return actualProvider(event).getWidgetset(event);
        }

        @Override
        public boolean isPreservedOnRefresh(UICreateEvent event) {
            return actualProvider(event).isPreservedOnRefresh(event);
        }
    }

    /**
     * Helper that installs a UI provider that selects the right theme when a UI
     * is initialized.
     */
    public static class ThemeCookieInitializer
            implements VaadinServiceInitListener {
        @Override
        public void serviceInit(ServiceInitEvent event) {
            event.getSource().addSessionInitListener(sessionInit -> {
                VaadinSession session = sessionInit.getSession();
                session.addUIProvider(new ProxyUiProvider(session));
            });
        }
    }

    /**
     * Sets a theme for the current UI and adds a cookie with the selection to
     * the current response.
     * 
     * @param themeName
     *            the theme name to set, or <code>null</code> to clear any
     *            existing cookie (without changing the UI theme).
     */
    public static void setTheme(String themeName) {
        VaadinResponse currentResponse = VaadinService.getCurrentResponse();
        UI currentUi = UI.getCurrent();

        if (currentResponse == null || currentUi == null) {
            if (currentUi != null
                    && currentUi.getPushConfiguration()
                            .getPushMode() != PushMode.DISABLED
                    && currentUi.getPushConfiguration()
                            .getTransport() == Transport.WEBSOCKET) {
                throw new IllegalStateException(
                        "Cannot be used together with regular websockets. Use Transport.WEBSOCKET_XHR instead.");
            } else {
                throw new IllegalStateException(
                        "Must be called during regular request handling and not from a background thread.");
            }
        }

        setTheme(themeName, currentUi, currentResponse);
    }

    /**
     * Sets a theme for the given UI and adds a cookie with the selection to the
     * given response.
     * 
     * @param themeName
     *            the theme name to set, or <code>null</code> to clear any
     *            existing cookie (without changing the UI theme).
     */
    public static void setTheme(String themeName, UI ui,
            VaadinResponse response) {
        Cookie cookie = new Cookie(THEME_COOKIE, themeName);
        cookie.setPath("/");

        if (themeName == null) {
            cookie.setMaxAge(0);
        } else {
            ui.setTheme(themeName);
            cookie.setMaxAge(Integer.MAX_VALUE);
        }

        response.addCookie(cookie);
    }

    private static Optional<String> findThemeCookieValue(
            VaadinRequest request) {
        return Stream.of(request.getCookies())
                .filter(cookie -> cookie.getName().equals(THEME_COOKIE))
                .map(Cookie::getValue).filter(ThemeCookie::validThemeName)
                .findAny();
    }

    private static boolean validThemeName(String themeName) {
        if (themeName == null || themeName.trim().isEmpty()
                || themeName.contains("/") || themeName.contains("..")) {
            return false;
        }

        String themeDir = "/VAADIN/themes/" + themeName.trim();
        return resourceExists(themeDir + "/styles.css")
                || resourceExists(themeDir + "/styles.scss");
    }

    private static boolean resourceExists(String name) {
        return ThemeCookie.class.getClassLoader().getResource(name) != null;
    }
}
