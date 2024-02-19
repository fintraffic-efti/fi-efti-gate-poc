from logging.config import dictConfig as loggingDictConfig

from flask import Flask

loggingDictConfig({
    'version': 1,
    'root': {
        'level': 'INFO',
    }
})

api = Flask(__name__)
