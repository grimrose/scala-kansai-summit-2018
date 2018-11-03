FROM boxfuse/flyway:5-alpine

RUN apk --update add postgresql-client bash && \
    rm -rf /var/cache/apk/*

COPY ./entrypoint.sh /flyway/entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]
