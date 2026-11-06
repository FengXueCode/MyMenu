"""本地 Mock AI 服务：用于没有真实 API Key 时自测 AI 提取/摘要/配图链路。

用法：
    python scripts/mock-ai-server.py            # 监听 http://127.0.0.1:9999/v1

在网页「AI 配置」里新增：
    Base URL: http://127.0.0.1:9999/v1
    对话模型: mock-chat
    图像模型: mock-image
    API Key: 任意字符串

仅用于本地开发，不参与生产运行。
"""
import base64
import json
from http.server import BaseHTTPRequestHandler, HTTPServer

RECIPE = {
    "title": "青椒炒肉丝",
    "description": "青椒脆嫩、肉丝滑香，十分钟搞定的下饭家常菜。",
    "categoryName": "快手菜",
    "meatType": "MEAT",
    "difficulty": 1,
    "servings": 2,
    "cookMinutes": 15,
    "spiceLevel": 1,
    "tags": ["快手", "下饭", "家常"],
    "ingredients": [
        {"name": "青椒", "amount": "200", "unit": "g"},
        {"name": "猪里脊", "amount": "150", "unit": "g"},
        {"name": "生抽", "amount": "1", "unit": "勺"},
        {"name": "淀粉", "amount": "5", "unit": "g"},
    ],
    "steps": [
        "里脊切细丝，加生抽和淀粉抓匀，腌制10分钟。",
        "青椒去籽切丝，蒜切末备用。",
        "热锅凉油下肉丝，大火滑炒至变色盛出。",
        "余油爆香蒜末，下青椒大火翻炒1分钟。",
        "倒回肉丝，加盐调味，翻炒30秒出锅。",
    ],
    "tips": "肉丝先腌后炒，口感更滑嫩。",
}

SUMMARY = {
    "summary": "青椒脆嫩肉丝滑香，十分钟上桌的快手下饭菜。",
    "ingredientsBrief": ["青椒", "猪里脊", "生抽"],
    "pairing": "配一碗白米饭，再来份番茄蛋汤。",
}

# 1x1 透明 PNG
PNG_BASE64 = (
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9Z0Y8AAAAASUVORK5CYII="
)


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        print("[mock-ai]", fmt % args, flush=True)

    def _json(self, payload, status=200):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        raw = self.rfile.read(length).decode("utf-8") if length else "{}"
        try:
            request = json.loads(raw)
        except json.JSONDecodeError:
            request = {}
        path = self.path.split("?")[0]
        if path.endswith("/chat/completions"):
            messages = request.get("messages") or []
            system = messages[0].get("content", "") if messages else ""
            content = SUMMARY if "配菜顾问" in system else RECIPE
            self._json({
                "id": "chatcmpl-mock",
                "object": "chat.completion",
                "model": request.get("model", "mock-chat"),
                "choices": [{
                    "index": 0,
                    "message": {"role": "assistant", "content": json.dumps(content, ensure_ascii=False)},
                    "finish_reason": "stop",
                }],
                "usage": {"prompt_tokens": 10, "completion_tokens": 10, "total_tokens": 20},
            })
        elif path.endswith("/images/generations"):
            self._json({"created": 1, "data": [{"b64_json": PNG_BASE64}]})
        else:
            self._json({"error": {"message": "not found"}}, 404)


if __name__ == "__main__":
    print("Mock AI server: http://127.0.0.1:9999/v1", flush=True)
    HTTPServer(("127.0.0.1", 9999), Handler).serve_forever()
