Relearn theme customizations now live in the site, not inside the vendored theme.

Current vendored theme:

theme = "hugo-theme-relearn-9.0.3"

Customizations carried forward from the previous "rpatrick" update:

1. Sidebar logo branding
   - The old theme edit replaced the default logo partial.
   - It is still applicable, and is now implemented with
     documentation/site/layouts/partials/logo.html plus
     documentation/site/layouts/partials/custom-header.html instead of editing
     the vendored theme.

2. Optional page H1 suppression
   - The old theme edit added support for page front matter "hideHeader: true".
   - It is still applicable and now lives in
     documentation/site/layouts/partials/heading.html.

3. Sidebar footer branding and links
   - The old theme edit replaced the theme's menu footer.
   - It is still applicable and now lives in
     documentation/site/layouts/partials/menu-footer.html.

4. External links opening in a new tab
   - The old theme edit replaced the Markdown render-link hook.
   - It is no longer carried forward as a template override.
   - The equivalent behavior is now set in documentation/site/config.toml with:
     externalLinkTarget = "_blank"

When installing a newer Relearn version, vendor the new release under
documentation/site/themes, update the theme setting in documentation/site/config.toml,
and verify that these site-level overrides still render correctly.

Use a Hugo version compatible with Relearn 9.x for validation.  The GitHub Pages
workflow currently downloads Hugo 0.161.1, which works with this upgrade.
