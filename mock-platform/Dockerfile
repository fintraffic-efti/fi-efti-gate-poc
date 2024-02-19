FROM python:3.12-alpine

COPY ./requirements.txt /app/requirements.txt

WORKDIR /app

RUN pip install -r requirements.txt

COPY ./src/main/python/*.py /app/

ENTRYPOINT [ "python" ]

CMD ["mock_platform.py"]
