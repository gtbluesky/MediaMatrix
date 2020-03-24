// 左右镜像（Y轴对称，取[0, 1/2]部分，左正右反）
precision highp float;
uniform sampler2D uTextureUnit;
varying highp vec2 vTextureCoord;

void main() {
    vec2 uv = vTextureCoord.xy;
    if (uv.x > 0.5) {
        uv.x = 1.0 - uv.x;
    }
    gl_FragColor = texture2D(uTextureUnit, uv);
}