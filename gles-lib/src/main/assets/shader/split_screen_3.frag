// 三分屏（上中下，取中间[1/3, 2/3]部分）
precision highp float;
uniform sampler2D uTextureUnit;
varying highp vec2 vTextureCoord;

void main() {
    highp vec2 uv = vTextureCoord;
    if (uv.y < 1.0 / 3.0) {
        uv.y = uv.y + 1.0 / 3.0;
    } else if (uv.y > 2.0 / 3.0) {
        uv.y = uv.y - 1.0 / 3.0;
    }
    gl_FragColor = texture2D(uTextureUnit, uv);
}