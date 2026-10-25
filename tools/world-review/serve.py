#!/usr/bin/env python3
"""Serve the review locally; never starts Minecraft or accesses source worlds."""
import argparse,http.server,pathlib
ap=argparse.ArgumentParser();ap.add_argument('directory',type=pathlib.Path);ap.add_argument('--port',type=int,default=8765);a=ap.parse_args()
class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self,*args,**kwargs):super().__init__(*args,directory=str(a.directory.resolve()),**kwargs)
    def end_headers(self):
        self.send_header('Cache-Control','no-cache');super().end_headers()
    def log_message(self,*args):pass
s=http.server.ThreadingHTTPServer(('127.0.0.1',a.port),Handler);print(f'Local: http://127.0.0.1:{a.port}',flush=True)
try:s.serve_forever()
except KeyboardInterrupt:s.server_close()
