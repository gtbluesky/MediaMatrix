// 仿抖音缩放特效
precision mediump float;
varying vec2 vTextureCoord;
uniform sampler2D uTextureUnit;

uniform float scale;

void main() {
    vec2 uv = vTextureCoord.xy;
    vec2 center = vec2(0.5, 0.5);
    uv = (uv - center) / scale + center;
    gl_FragColor = texture2D(uTextureUnit, uv);
}