/* Máscara de CPF/CNPJ e consulta de CNPJ pelo servidor (/api/cnpj). */
window.CpfCnpj = {
  formatar: function (valor) {
    var v = String(valor || '').replace(/\D/g, '').slice(0, 14);
    if (v.length <= 11) return v.replace(/(\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d{1,2})$/, '$1-$2');
    return v.replace(/^(\d{2})(\d)/, '$1.$2').replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
            .replace(/\.(\d{3})(\d)/, '.$1/$2').replace(/(\d{4})(\d)/, '$1-$2');
  },
  mascara: function (input) {
    var self = this;
    input.addEventListener('input', function () { input.value = self.formatar(input.value); });
  },
  consultar: function (valor) {
    var d = String(valor || '').replace(/\D/g, '');
    if (d.length !== 14) return Promise.reject(new Error('Digite um CNPJ com 14 dígitos para buscar.'));
    return fetch('/api/cnpj/' + d, { headers: { 'Accept': 'application/json' } }).then(function (r) {
      return r.json().catch(function () { return {}; }).then(function (j) {
        if (!r.ok) throw new Error(j.erro || 'Não foi possível consultar o CNPJ agora.');
        return j;
      });
    });
  }
};
