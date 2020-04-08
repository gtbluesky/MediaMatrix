// 仿抖音毛刺特效
precision highp float;
varying vec2 vTextureCoord;
uniform sampler2D uTextureUnit;
uniform float uMaxOffset;
uniform float uThreshold;
uniform float uColorDrift;

float random(vec2 coord) {
    return fract(sin(dot(coord, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    float x = vTextureCoord.x;
    float y = vTextureCoord.y;
    // 得到一个范围在[-1,1]之间的数
    float jitter = random(vec2(y, 0.0)) * 2.0 - 1.0;
    // step(a, b) = (a > b) ? 0 : 1
    // 超过阈值为1，否则为0
    float offset = step(uThreshold, abs(jitter));
    float jitterOffset = jitter * offset * uMaxOffset;
    vec4 color1 = texture2D(uTextureUnit, fract(vec2(x + jitterOffset, y)));
    // uColorDrift * (1.0 - y) 是模仿抖音颜色倾斜偏移效果
    vec4 color2 = texture2D(uTextureUnit, fract(vec2(x + jitterOffset - uColorDrift * (1.0 - y), y)));
    gl_FragColor = vec4(color1.r, color2.g, color1.b, 1.0);
}