# Testing and building seriesgui.de

This is a website built with [Jekyll](https://jekyllrb.com/) on [GitHub Actions](/.github/workflows/publish-website.yml) published using [GitHub Pages](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site#publishing-with-a-custom-github-actions-workflow).

Note the [special instructions from Jekyll for GitHub Pages](https://jekyllrb.com/docs/github-pages/).

[Liquid syntax](https://shopify.github.io/liquid/basics/introduction/)

[Collections](https://jekyllrb.com/docs/collections/) (used for help pages)

## Development

Dependencies are managed with Ruby [bundler](https://bundler.io/), see [Gemfile](Gemfile) and [Gemfile.lock](Gemfile.lock).

The local Ruby version should match the [CI config](.github/workflows/publish-website.yml).

Using [rbenv](https://github.com/rbenv/rbenv):

```bash
sudo apt install rbenv
rbenv init
rbenv install 4.0.1
bundle install
```

Other useful commands:

```bash
# change .ruby-version
rbenv local 4.0.1

# update gems
bundle update --all

# update bundler
bundle update --bundler

# look for outdated gems
gem outdated

# test locally
bundle exec jekyll serve
```

## Updating Bootstrap

Current Bootstrap version: `5.3.8`

[Download source of latest release](https://github.com/twbs/bootstrap/releases).

- Clean `_sass\bootstrap` folder.
- From `scss` folder copy into `_sass\bootstrap`.
- Check `css\main.scss` if updates are required (e.g. names of components or variables have changed).
- From `dist\js` folder copy `bootstrap.min.js` and `.map` into `javascripts`.
- Follow [migration guide](https://getbootstrap.com/docs/5.3/migration/).

## Configuration

DNS:

- CNAME `seriesgui.de.` to `uwetrottmann.github.io.`, Proxied so Page Rules work
- CNAME `www.seriesgui.de.` to `uwetrottmann.github.io.`, DNS only to directly access GitHubs IP

Page rules:

- URL: `seriesgui.de/*`, Forwarding URL: 301 permanent, Destination: `https://www.seriesgui.de/$1`
