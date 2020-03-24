// 二分屏（上下，取中间[1/4, 3/4]部分）
precision highp float;
uniform sampler2D uTextureUnit;
varying highp vec2 vTextureCoord;

void main() {
    vec2 uv = vTextureCoord.xy;
    float y;
    if (uv.y >= 0.0 && uv.y <= 0.5) {
        y = uv.y + 0.25;
    } else {
        y = uv.y - 0.25;
    }
    gl_FragColor = texture2D(uTextureUnit, vec2(uv.x, y));
}