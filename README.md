scala-kansai-summit-2018
=============================

[![CircleCI](https://circleci.com/gh/grimrose/scala-kansai-summit-2018/tree/master.svg?style=svg)](https://circleci.com/gh/grimrose/scala-kansai-summit-2018/tree/master)

[![codecov](https://codecov.io/gh/grimrose/scala-kansai-summit-2018/branch/master/graph/badge.svg)](https://codecov.io/gh/grimrose/scala-kansai-summit-2018)

## Akkaを分散トレーシングで見てみよう

[slide](http://nbviewer.jupyter.org/format/slides/github/grimrose/scala-kansai-summit-2018/blob/master/slide.ipynb#/)

### requirements

* docker
* docker-compose
* sbt
* pipenv
* [kt3k/saku](https://github.com/kt3k/saku)

### usage

[commands](saku.md)

### setup

```bash
$ cp .env.sample .env
```

```bash
$ pipenv install
```

### demo 

```bash
$ saku stage
```

```bash
$ saku start
```
