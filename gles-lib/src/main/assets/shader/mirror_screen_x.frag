// 上下镜像（X轴对称，取[1/2, 1]部分，上正下反）
precision highp float;
uniform sampler2D uTextureUnit;
varying highp vec2 vTextureCoord;

void main() {
    vec2 uv = vTextureCoord.xy;
    if (uv.y < 0.5) {
        uv.y = 1.0 - uv.y;
    }
    gl_FragColor = texture2D(uTextureUnit, uv);
}