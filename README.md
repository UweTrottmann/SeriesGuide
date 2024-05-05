# Testing and building seriesgui.de

This is a [GitHub Pages](https://help.github.com/categories/github-pages-basics/) website built with [Jekyll](https://jekyllrb.com/).

Note the special instructions for [GitHub Pages](https://jekyllrb.com/docs/github-pages/).

https://help.github.com/articles/setting-up-your-github-pages-site-locally-with-jekyll


```
# update gems
bundle update

# look for outdated gems (notably github-pages)
gem outdated

# test locally
bundle exec jekyll serve
```

## Updating Bootstrap
[Download source of latest release](https://github.com/twbs/bootstrap/releases).

* Clean `_sass\bootstrap` folder.
* From `scss` folder copy into `_sass\bootstrap`.
* Check `css\main.scss` if updates are required (e.g. names in `_sass\bootstrap\bootstrap.scss` have changed).
* From `dist\js` folder copy `bootstrap.min.js` and `.map` into `javascripts`.

## Configuration

DNS:
- CNAME `seriesgui.de.` to `uwetrottmann.github.io.`, Proxied so Page Rules work
- CNAME `www.seriesgui.de.` to `uwetrottmann.github.io.`, DNS only to directly access GitHubs IP

Page rules:
- URL: `seriesgui.de/*`, Forwarding URL: 301 permanent, Destination: `https://www.seriesgui.de/$1`
