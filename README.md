# Testing and building seriesgui.de

This is a website built with [Jekyll](https://jekyllrb.com/) on [GitHub Actions](/.github/workflows/publish-website.yml) published using [GitHub Pages](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site#publishing-with-a-custom-github-actions-workflow).

Note the [special instructions from Jekyll for GitHub Pages](https://jekyllrb.com/docs/github-pages/).

https://help.github.com/articles/setting-up-your-github-pages-site-locally-with-jekyll

[Liquid syntax](https://shopify.github.io/liquid/basics/introduction/)

[Collections](https://jekyllrb.com/docs/collections/) (used for help pages)

```bash
# update gems
bundle update

# look for outdated gems (notably github-pages)
gem outdated

# test locally
bundle exec jekyll serve
```

## Updating Bootstrap

[Download source of latest release](https://github.com/twbs/bootstrap/releases).

- Clean `_sass\bootstrap` folder.
- From `scss` folder copy into `_sass\bootstrap`.
- Check `css\main.scss` if updates are required (e.g. names in `_sass\bootstrap\bootstrap.scss` have changed).
- From `dist\js` folder copy `bootstrap.min.js` and `.map` into `javascripts`.

## Configuration

DNS:

- CNAME `seriesgui.de.` to `uwetrottmann.github.io.`, Proxied so Page Rules work
- CNAME `www.seriesgui.de.` to `uwetrottmann.github.io.`, DNS only to directly access GitHubs IP

Page rules:

- URL: `seriesgui.de/*`, Forwarding URL: 301 permanent, Destination: `https://www.seriesgui.de/$1`
