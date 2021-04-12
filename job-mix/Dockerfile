FROM python:3.6
LABEL Version=1.0

WORKDIR /app
ADD run.sh eval_jobmix.py requirements.txt process_commands.py /app/

RUN pip install --upgrade pip
RUN pip install --upgrade -r requirements.txt

RUN mkdir log
RUN chmod o+rw log

ENTRYPOINT /app/run.sh
