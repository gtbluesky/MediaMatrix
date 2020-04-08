// 仿抖音闪白特效
precision highp float;
varying vec2 vTextureCoord;
uniform sampler2D uTextureUnit;
uniform float uPercent;

void main() {
    vec4 color = texture2D(uTextureUnit, vTextureCoord);
    gl_FragColor = vec4(
        color.r + (1.0 - color.r) * uPercent,
        color.g + (1.0 - color.g) * uPercent,
        color.b + (1.0 - color.b) * uPercent,
        color.a
    );
}