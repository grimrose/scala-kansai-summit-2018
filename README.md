scala-kansai-summit-2018
=============================

## Akkaを分散トレーシングで見てみよう

[slide](http://nbviewer.jupyter.org/format/slides/github/grimrose/scala-kansai-summit-2018/blob/3fcaec635c392323fcd17ebcdd2c70bdf8ced18d/slide.ipynb#/)

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
