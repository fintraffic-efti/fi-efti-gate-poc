import os
from flask_app import api
from flask import request, json, send_from_directory


@api.route('/api/v1/consignments/<data_id>', methods=['GET'])
def get_with_parameter(data_id):
    response = {
        'country-start-id': 'FI',
        'data-id': data_id,
    }
    return response, 200


@api.route('/example/get-from-disk', methods=['GET'])
def get_from_disk():
    return send_from_directory(
        api.config['DATA_DIR'], 'example.json', as_attachment=False
    )


@api.route('/example/post-json', methods=['POST'])
def post_json():
    dto = request.json
    api.logger.info(json.dumps(dto, indent=4))

    return dto, 200


def get_default_data_dir():
    relative_path = './data/'
    absolute_path = os.path.join(os.getcwd(), relative_path)
    return absolute_path


def get_data_dir():
    absolute_path = os.getenv(
        'MOCK_PLATFORM_DATA_DIR',
        get_default_data_dir()
    )
    return absolute_path


if __name__ == '__main__':
    dir = get_data_dir()
    print('Serving files from path: %s' % dir)
    api.config['DATA_DIR'] = dir
    api.run(host='0.0.0.0', port=8091)
