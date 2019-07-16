# wor-wiki-bot

A bot that posts recent activity in BookStacks to a webhook.

## How it works

BookStacks currently doesn't have a public API, so this bot hooks into its
database directly. It runs every minute, and posts all activity from that minute
to the webhook URL. If it is offline, it will miss activity. However, this
allows it to be stateless.

## Configuration

See `config.edn.example` for an example configuration:

``` clojure
{:webhook/url #env WWB_WEBHOOK_URL
:database/uri #env WWB_DATABASE_URI
:website/url #env WWB_WEBSITE_URL}
```

The webhook URL is the URL to post new updates to (get it from Discord or Slack
or wherever, I only tested it with Discord.) The database URI is a JDBC database
connection to the MariaDB instance that BookStack is using. The website URL is
used to construct links to the entities that were changed. This probably only
works on page updates.

## Usage

To run the bot,

``` shell
$ clj -A:run
```

To download dependencies,

``` shell
% clj -A:run -e :ok
```

# License

    Copyright (C) 2019 Andrew Amis

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
