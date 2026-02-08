FROM python:3
ENV PYTHONUNBUFFERED 1
ADD . /home/app
WORKDIR /home/app
RUN pip install -r requirements.txt
#RUN ls -l
#CMD [ "pwd" ]
#CMD [ "python", "manage.py runserver 0.0.0.0:8000"
EXPOSE 8000
ENTRYPOINT python manage.py runserver 0.0.0.0:8000