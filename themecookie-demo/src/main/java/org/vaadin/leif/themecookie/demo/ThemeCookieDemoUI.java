package org.vaadin.leif.themecookie.demo;

import java.util.Arrays;

import javax.servlet.annotation.WebServlet;

import org.vaadin.leif.themecookie.ThemeCookie;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Theme("valo")
public class ThemeCookieDemoUI extends UI {

    private static final String[] themes = { "valo", "reindeer", "runo" };

    @Override
    protected void init(VaadinRequest request) {
        ComboBox<String> themeSelector = new ComboBox<>("Current theme",
                Arrays.asList(themes));
        themeSelector.setEmptySelectionAllowed(false);

        themeSelector.setValue(getTheme());

        themeSelector.addValueChangeListener(valueChange -> {
            ThemeCookie.setTheme(valueChange.getValue());
        });

        Button clearButton = new Button("Clear cookie",
                click -> ThemeCookie.setTheme(null));

        setContent(new VerticalLayout(themeSelector, clearButton));
    }

    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = ThemeCookieDemoUI.class)
    public static class Servlet extends VaadinServlet {
    }
}
