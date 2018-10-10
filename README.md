# Theme Cookie add-on

This Vaadin Framework add-on that saves the user's theme preference in a cookie and automatically uses that theme when opening the application.

## Usage
Instead of `ui.setTheme(selectedTheme);`, do `ThemeCookie.setTheme(selectedTheme);` and everything else will just  automatically work.

```
themeSelector.addValueChangeListener(valueChange -> {
    ThemeCookie.setTheme(valueChange.getValue());
});
```

This will update the theme of the current UI and set a cookie. When the user opens the application again, the add-on will automatically read the cookie and configure the same theme to be used.
