precision highp float;
uniform sampler2D uTextureUnit;
varying highp vec2 vTextureCoord;
uniform float uViewWidth;
uniform float uViewHeight;
uniform float uMosaicSize;

void main() {
    vec2 uv = vTextureCoord.xy;
    float dx = uMosaicSize / uViewWidth;
    float dy = uMosaicSize / uViewHeight;
    gl_FragColor = texture2D(uTextureUnit, vec2(dx * floor(uv.x / dx), dy * floor(uv.y / dy)));
}