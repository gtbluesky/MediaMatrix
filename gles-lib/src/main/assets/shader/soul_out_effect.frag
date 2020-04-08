// 仿抖音灵魂出窍特效
precision mediump float;
varying vec2 vTextureCoord;
uniform sampler2D uTextureUnit;

uniform float scale;

void main() {
    vec2 uv = vTextureCoord.xy;
    // 输入纹理
    vec4 sourceColor = texture2D(uTextureUnit, fract(uv));
    vec2 center = vec2(0.5, 0.5);
    uv = (uv - center) / scale + center;

    // 缩放纹理
    vec4 scaleColor = texture2D(uTextureUnit, fract(uv));
    // 线性混合
    gl_FragColor = mix(sourceColor, scaleColor, 0.5 * (0.6 - fract(scale)));
}